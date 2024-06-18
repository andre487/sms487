#!/usr/bin/env bash
set -eufo pipefail

IFS=$'\n' read -r -d '' -a images_list < <(
    docker images --all --no-trunc |
        tail -n +2 |
        grep -v ' latest ' |
        sort &&
        printf '\0'
)

images_count="${#images_list[@]}"
if ((images_count <= 3)); then
    echo "We have only $images_count images, don't clean"
    exit 0
fi

stop_select=$((images_count - 3))
slice_to_delete=("${images_list[@]:0:stop_select}")

names_to_delete=()
for image in "${slice_to_delete[@]}"; do
    name="$(awk '{x=$1":"$2; print x}' <<<"$image")"
    names_to_delete=("$name" "${names_to_delete[@]+"${names_to_delete[@]}"}")
done

docker rmi "${names_to_delete[@]}"
