#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
infra_dir=$(CDPATH= cd -- "${script_dir}/.." && pwd)
env_file="${infra_dir}/.env"

if [ ! -f "${env_file}" ]; then
    echo "${env_file} 파일이 없습니다." >&2
    exit 1
fi

docker compose \
    --project-directory "${infra_dir}" \
    --env-file "${env_file}" \
    --file "${infra_dir}/compose.yaml" \
    down

echo "Jenkins 컨테이너를 중지했습니다. Jenkins Home 볼륨은 보존됩니다."
