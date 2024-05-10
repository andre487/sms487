#!/usr/bin/env bash
set -euo pipefail

docker_tag="${1:-latest}"

cur_dir="$(cd "$(dirname "$0")" && pwd)"
source "$cur_dir/common.sh"

IFS=$'\n' read -r -d '' -a instance_list < <(get_instances && printf '\0')

for instance in "${instance_list[@]}"; do
    echo "Update container on $instance to version $docker_tag"
    yc compute instance update-container "$instance" \
        --container-image "$DOCKER_IMAGE:$docker_tag"
done
