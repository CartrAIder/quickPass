pipeline {
    agent {
        label 'docker_agent'
    }

    options {
        timestamps()
        disableConcurrentBuilds()

        // Jenkins가 자동 Checkout하는 것을 막는다.
        // 아래 Checkout stage에서 직접 수행하기 때문.
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
                        set -a
                        . "$SPRING_ENV_FILE"
                        set +a

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
                retry(12) {
                    timeout(time: 10, unit: 'SECONDS') {
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
            sh 'docker logs --tail 200 quickpass-backend || true'
        }

        always {
            cleanWs()
        }
    }
}
