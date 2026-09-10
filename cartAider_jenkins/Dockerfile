ARG JENKINS_IMAGE_TAG=lts-jdk21
FROM jenkins/jenkins:${JENKINS_IMAGE_TAG}

USER root

# Jenkinsfile에서 사용하는 Docker CLI, Compose, curl을 설치한다.
RUN set -eux; \
    apt-get update; \
    apt-get install --yes --no-install-recommends ca-certificates curl gnupg; \
    install -m 0755 -d /etc/apt/keyrings; \
    curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc; \
    chmod a+r /etc/apt/keyrings/docker.asc; \
    . /etc/os-release; \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian ${VERSION_CODENAME} stable" > /etc/apt/sources.list.d/docker.list; \
    apt-get update; \
    apt-get install --yes --no-install-recommends docker-ce-cli docker-buildx-plugin docker-compose-plugin; \
    rm -rf /var/lib/apt/lists/*

COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

COPY --chown=jenkins:jenkins casc/jenkins.yaml /usr/share/jenkins/ref/casc/jenkins.yaml

USER jenkins
