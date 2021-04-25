export IAM_TOKEN="$(yc iam create-token)"
export PROJECT_DIR="$(realpath "$(cd "$(dirname "$0")/../.." && pwd)")"
export SECRET_DIR="$PROJECT_DIR/.secret"
export LOCKBOX_SECRET_URL=https://payload.lockbox.api.cloud.yandex.net/lockbox/v1/secrets

export SERVICE_ACCOUNT=robot-service
export MAIN_USER=andre487

export CONTAINER_NAME=sms487-api
export DOCKER_IMAGE=cr.yandex/crp998oqenr95rs4gf9a/sms487-api
export DOCKER_IMAGE_LATEST="$DOCKER_IMAGE:latest"
export SSH_KEY_SECRET=e6q6vfo2vo565h0cb0nq

get_main_user_public_key() {
    local secret_file
    local payload

    secret_file="$SECRET_DIR/main_user_id_rsa.pub"

    if [[ ! -f "$secret_file" ]]; then
        payload="$(curl -s --fail -H "Authorization: Bearer $IAM_TOKEN" "$LOCKBOX_SECRET_URL/$SSH_KEY_SECRET/payload")"
        jq <<< "$payload" -r '.entries | map(select(.key == "id_rsa.pub")) | .[0].textValue' >"$secret_file"
    fi

    echo "$secret_file"
}

get_prod_machine() {
    zone="$1"

    yc compute instance list --format json | \
    jq -r "
        map(select(.labels.machine_type == \"sms487-api\" and .zone_id == \"$zone\")) |
        first |
        .network_interfaces | first |
        .primary_v4_address.one_to_one_nat.address
    "
}
