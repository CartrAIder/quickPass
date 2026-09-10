# QuickPass Jenkins 인프라

현재 저장소의 `Jenkinsfile`을 로컬 또는 신뢰된 개발 서버에서 실행하기 위한 컨테이너 구성이다. Jenkins LTS와 Java 21을 사용하며, 컨테이너 안에 Docker CLI·Compose와 파이프라인 필수 플러그인을 설치한다.

Jenkins 컨테이너는 Controller와 실행 노드를 겸한다. JCasC가 내장 노드에 `quickpass-docker-agent` 라벨을 설정하므로 별도 Agent 등록 없이 현재 `Jenkinsfile`을 실행할 수 있다. Jenkins Home과 Gradle 캐시는 `cartaider-jenkins-home` 볼륨에 유지된다.

## 사전 조건

- Docker Desktop 또는 Docker Engine
- Docker Compose 플러그인
- QuickPass 저장소에 접근할 수 있는 Git 자격 증명
- 배포에 사용할 QuickPass 애플리케이션 `.env`

## 시작

```bash
cd cartAider_jenkins
cp .env.example .env
```

`.env`의 `JENKINS_ADMIN_PASSWORD`를 기본값이 아닌 충분히 긴 값으로 변경한 뒤 실행한다.

```bash
./scripts/start.sh
```

스크립트는 다음 작업을 수행한다.

1. Docker 소켓 그룹 ID를 Jenkins 컨테이너에 전달한다.
2. 배포용 `cartAider-network`가 없으면 생성한다.
3. Jenkins 이미지를 빌드하고 컨테이너를 시작한다.

기본 접속 주소는 `http://localhost:8081`이다. 로그인 정보는 `cartAider_jenkins/.env`에 지정한 관리자 계정을 사용한다.

## QuickPass 파이프라인 등록

1. Jenkins에서 **New Item → Pipeline**을 선택한다.
2. Pipeline 정의를 **Pipeline script from SCM**으로 설정한다.
3. SCM은 Git, Repository URL은 이 저장소 URL을 입력한다.
4. 비공개 저장소라면 Jenkins Credentials에 Git 자격 증명을 추가해 선택한다.
5. 빌드할 브랜치와 Script Path `Jenkinsfile`을 지정한다.

현재 `Jenkinsfile`의 배포 단계는 `quickpass-production-env`라는 Jenkins Secret file credential을 요구한다. **Manage Jenkins → Credentials → System → Global credentials**에서 배포 환경 변수 파일을 Secret file로 등록하고 ID를 정확히 `quickpass-production-env`로 지정한다. 비밀 값은 Git이나 Jenkinsfile에 직접 넣지 않는다.

애플리케이션 배포 전에 MySQL, Redis, MinIO, MQTT 등 의존 컨테이너가 루트 `docker-compose.yml`을 통해 `cartAider-network`에 실행 중이어야 한다. Jenkins가 배포한 백엔드는 같은 네트워크에서 `spring` 별칭을 사용하며, Jenkins Health Check도 `http://spring:8080/v3/api-docs`를 확인한다.

## 관리 명령

```bash
# 상태와 로그 확인
docker ps --filter name=cartaider-jenkins
docker logs --follow cartaider-jenkins

# Jenkins 중지(데이터 볼륨 유지)
./scripts/stop.sh

# 설정 또는 플러그인 변경 후 다시 빌드
./scripts/start.sh
```

JCasC의 관리자 비밀번호는 최초 생성 시 Jenkins 사용자 DB에 반영된다. 이미 생성된 `cartaider-jenkins-home` 볼륨에서 `.env`만 바꿔도 기존 관리자 비밀번호가 자동 변경되지는 않는다.

## 보안 주의사항

이 구성은 현재 Jenkinsfile이 호스트 Docker 엔진에서 애플리케이션 컨테이너를 교체할 수 있도록 Docker 소켓을 마운트한다. Docker 소켓 접근은 사실상 호스트 관리자 권한과 같으므로 다음 원칙을 지킨다.

- 신뢰된 관리자와 저장소만 Jenkins에 등록한다.
- Jenkins 포트를 공용 인터넷에 직접 노출하지 않는다.
- 관리자·SCM·배포 자격 증명을 서로 분리하고 정기적으로 교체한다.
- 운영 환경에서는 Controller와 Agent를 분리하고 전용 Agent 호스트 또는 격리된 빌드 런타임을 사용한다.

Jenkins 데이터를 완전히 초기화하려면 컨테이너를 내린 뒤 `cartaider-jenkins-home` 볼륨을 별도로 삭제해야 한다. 이 작업은 Job, Credentials, 빌드 기록을 모두 제거하므로 백업 후 명시적으로 수행한다.
