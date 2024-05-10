#!/usr/bin/env bash
set -euo pipefail

zone="${ZONE:-ru-central1-d}"
machine_name="sms487-api-$zone"

cur_dir="$(cd "$(dirname "$0")" && pwd)"
source "$cur_dir/common.sh"

yc compute instance create-with-container \
    --name "$machine_name" \
    --description "SMS487 Data API" \
    --labels "machine_type=sms487-api" \
    --zone="$zone" \
    --ssh-key "$(get_main_user_public_key)" \
    --service-account-name "$SERVICE_ACCOUNT" \
    --network-interface "subnet-name=default-$zone,nat-ip-version=ipv4" \
    --serial-port-settings ssh-authorization=instance_metadata \
    --cores 2 \
    --core-fraction 5 \
    --memory 1GB \
    --container-name "$CONTAINER_NAME" \
    --container-image "$DOCKER_IMAGE_LATEST" \
    --container-tty \
    --container-env-file "$cur_dir/container.env" \
    --container-restart-policy Always
