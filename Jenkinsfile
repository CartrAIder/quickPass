pipeline {
    // Jenkins Controller가 아닌 Docker 권한을 가진 Agent에서 실행한다.
    agent {
        label 'quickpass-docker-agent'
    }

    options {
        timestamps()
        // 동시에 두 배포가 실행되어 컨테이너 교체 순서가 꼬이는 것을 방지한다.
        disableConcurrentBuilds()
    }

    environment {
        BACKEND_HEALTH_URL = 'http://spring:8080/v3/api-docs'
    }

    stages {
        stage('Checkout') {
            steps {
                // Multibranch Pipeline 또는 Pipeline SCM 설정의 저장소를 checkout한다.
                checkout scm
            }
        }

        stage('Spring Test') {
            steps {
                sh './gradlew test --no-daemon'
            }
        }

        stage('Spring Build') {
            steps {
                sh './gradlew bootJar --no-daemon'
            }
        }

        stage('Docker Deploy') {
            steps {
                // Jenkins Credentials에 'quickpass-production-env' ID로 .env Secret file을 등록해야 한다.
                // 실제 비밀 값은 Git 저장소나 Jenkinsfile에 기록하지 않는다.
                withCredentials([file(credentialsId: 'quickpass-production-env', variable: 'DEPLOY_ENV_FILE')]) {
                    sh '''
                        set -eu
                        install -m 600 "$DEPLOY_ENV_FILE" .env
                        docker build --tag "quickpass-backend:$BUILD_NUMBER" .
                        docker rm --force quickpass-backend 2>/dev/null || true
                        docker run --detach \
                            --name quickpass-backend \
                            --network cartAider-network \
                            --network-alias spring \
                            --env-file .env \
                            --restart unless-stopped \
                            "quickpass-backend:$BUILD_NUMBER"
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                // Agent도 cartAider-network에 연결되어 있어 spring alias로 확인할 수 있다.
                retry(12) {
                    timeout(time: 10, unit: 'SECONDS') {
                        sh 'curl --fail --silent --show-error "$BACKEND_HEALTH_URL" > /dev/null'
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
            // 장애 원인 확인을 위한 최근 백엔드 로그만 출력한다.
            sh 'docker logs --tail 200 quickpass-backend || true'
        }
        always {
            // Jenkins 작업 공간에 복사한 배포 환경 파일을 남기지 않는다.
            sh 'rm -f .env'
            cleanWs()
        }
    }
}
