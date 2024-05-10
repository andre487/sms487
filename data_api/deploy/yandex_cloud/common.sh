IAM_TOKEN="$(yc iam create-token)"; export IAM_TOKEN
PROJECT_DIR="$(realpath "$(cd "$(dirname "$0")/../.." && pwd)")"; export PROJECT_DIR
export SECRET_DIR="$PROJECT_DIR/.secret"

export SERVICE_ACCOUNT=robot-service

export CONTAINER_NAME=sms487-api
export DOCKER_IMAGE=cr.yandex/crp998oqenr95rs4gf9a/sms487-api
export DOCKER_IMAGE_LATEST="$DOCKER_IMAGE:latest"

get_main_user_private_key() {
    echo "$SECRET_DIR/ssh/id_ecdsa"
}

get_main_user_public_key() {
    echo "$SECRET_DIR/ssh/id_ecdsa.pub"
}

get_instances() {
    yc compute instance list --format json |
    jq -r 'map(select(.labels.machine_type == "sms487-api").name) | .[0]'
}

get_zones() {
    yc compute instance list --format json |
    jq -r 'map(select(.labels.machine_type == "sms487-api").zone_id) | .[0]'
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
