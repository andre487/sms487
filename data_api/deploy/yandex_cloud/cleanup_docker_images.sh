#!/usr/bin/env bash
set -eufo pipefail

cur_dir="$(cd "$(dirname "$0")" && pwd)"
source "$cur_dir/common.sh"

cleanup_images_in_zone() {
    zone="$1"
    echo "Cleaning images in zone $zone"

    cur_dir="$(cd "$(dirname "$0")" && pwd)"
    source "$cur_dir/common.sh"

    ip_addr="$(get_prod_machine "$zone")"

    IFS=$'\n' read -r -d '' -a images_list < <(
        ssh -i "$(get_main_user_private_key)" \
            -o "StrictHostKeyChecking no" \
            "yc-user@$ip_addr" \
            'docker images --all --no-trunc' | \
        tail -n +2 | \
        grep -v ' latest ' | \
        sort && \
        printf '\0'
    )

    images_count="${#images_list[@]}"
    if (( images_count <= 3)); then
        echo "We have only $images_count images in zone $zone, don't clean"
        return 0
    fi

    stop_select=$(( images_count - 3 ))
    slice_to_delete=("${images_list[@]:0:stop_select}")

    names_to_delete=()
    for image in "${slice_to_delete[@]}"; do
        name="$(awk '{x=$1":"$2; print x}' <<< "$image")"
        names_to_delete=("$name" "${names_to_delete[@]+"${names_to_delete[@]}"}")
    done

    ssh -i "$(get_main_user_private_key)" \
        -o "StrictHostKeyChecking no" \
        "yc-user@$ip_addr" \
        "set -x; docker rmi ${names_to_delete[*]}"
}

IFS=$'\n' read -r -d '' -a zones < <(get_zones && printf '\0')

for zone in "${zones[@]}"; do
    cleanup_images_in_zone "$zone"
done
