pipeline {
    agent {
        label 'docker_agent'
    }

    options {
        timestamps()
        disableConcurrentBuilds()

        // Jenkins가 자동 Checkout하는 것을 막는다.
        // 아래 Checkout stage에서 직접 수행한다.
        skipDefaultCheckout(true)
    }

    environment {
        BACKEND_HEALTH_URL = 'http://spring:8080/v3/api-docs'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Spring Test') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'quickpass-production-env',
                        variable: 'SPRING_ENV_FILE'
                    )
                ]) {
                    sh '''
                        # Jenkins shell tracing을 끈다.
                        # Secret File 내부 값이 Console Log에 노출되는 것을 방지한다.
                        set +x
                        set -eu

                        # Docker --env-file 형식의 .env를
                        # shell source 하지 않고 안전하게 환경변수로 등록한다.
                        while IFS= read -r line || [ -n "$line" ]; do
                            case "$line" in
                                ''|'#'*)
                                    continue
                                    ;;
                            esac

                            key="${line%%=*}"
                            value="${line#*=}"

                            export "$key=$value"
                        done < "$SPRING_ENV_FILE"

                        ./gradlew test --no-daemon
                    '''
                }
            }
        }

        stage('Spring Build') {
            steps {
                sh './gradlew bootJar --no-daemon'
            }
        }

        stage('Docker Deploy') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'quickpass-production-env',
                        variable: 'DEPLOY_ENV_FILE'
                    )
                ]) {
                    sh '''
                        set +x
                        set -eu

                        docker build \
                            --tag "quickpass-backend:$BUILD_NUMBER" \
                            .

                        docker rm --force quickpass-backend \
                            2>/dev/null || true

                        docker run --detach \
                            --name quickpass-backend \
                            --network cartAider-network \
                            --network-alias spring \
                            --env-file "$DEPLOY_ENV_FILE" \
                            --restart unless-stopped \
                            "quickpass-backend:$BUILD_NUMBER"
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                retry(24) {
                    // Spring Boot가 초기화될 시간을 준다.
                    sleep time: 5, unit: 'SECONDS'

                    timeout(time: 5, unit: 'SECONDS') {
                        sh '''
                            curl \
                                --fail \
                                --silent \
                                --show-error \
                                "$BACKEND_HEALTH_URL" \
                                > /dev/null
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'QuickPass 백엔드 배포 및 Health Check가 완료되었습니다.'
        }

        failure {
            // 컨테이너가 존재하면 최근 로그를 확인한다.
            sh '''
                docker logs --tail 200 quickpass-backend \
                    2>/dev/null || true
            '''
        }

        cleanup {
            cleanWs()
        }
    }
}
