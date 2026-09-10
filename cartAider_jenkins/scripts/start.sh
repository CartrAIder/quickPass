#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
infra_dir=$(CDPATH= cd -- "${script_dir}/.." && pwd)
env_file="${infra_dir}/.env"
docker_socket=/var/run/docker.sock

if [ ! -f "${env_file}" ]; then
    echo "${env_file} 파일이 없습니다. .env.example을 복사하고 관리자 비밀번호를 변경하세요." >&2
    exit 1
fi

if grep -Eq '^JENKINS_ADMIN_PASSWORD=change-this-jenkins-admin-password$' "${env_file}"; then
    echo "기본 JENKINS_ADMIN_PASSWORD를 안전한 값으로 변경하세요." >&2
    exit 1
fi

if [ ! -S "${docker_socket}" ]; then
    echo "${docker_socket}을 찾을 수 없습니다. Docker Desktop 또는 Docker Engine을 먼저 실행하세요." >&2
    exit 1
fi

if ! docker_operating_system=$(docker info --format '{{.OperatingSystem}}' 2>/dev/null); then
    echo "Docker 데몬에 연결할 수 없습니다. Docker Desktop 또는 Docker Engine 상태를 확인하세요." >&2
    exit 1
fi

if printf '%s' "${docker_operating_system}" | grep -q 'Docker Desktop'; then
    # Docker Desktop은 호스트 소켓을 컨테이너 안에서 root:root(0660)로 제공한다.
    quickpass_docker_gid=0
elif stat -Lc '%g' "${docker_socket}" >/dev/null 2>&1; then
    quickpass_docker_gid=$(stat -Lc '%g' "${docker_socket}")
else
    quickpass_docker_gid=$(stat -Lf '%g' "${docker_socket}")
fi
export QUICKPASS_DOCKER_GID="${quickpass_docker_gid}"

if ! docker network inspect cartAider-network >/dev/null 2>&1; then
    docker network create cartAider-network >/dev/null
fi

docker compose \
    --project-directory "${infra_dir}" \
    --env-file "${env_file}" \
    --file "${infra_dir}/compose.yaml" \
    up --detach --build

echo "Jenkins 시작 요청이 완료되었습니다. 상태: docker ps --filter name=cartaider-jenkins"
