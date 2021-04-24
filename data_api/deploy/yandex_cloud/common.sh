export IAM_TOKEN="$(yc iam create-token)"
export PROJECT_DIR="$(realpath "$(cd "$(dirname "$0")/../.." && pwd)")"
export SECRET_DIR="$PROJECT_DIR/.secret"
export LOCKBOX_SECRET_URL=https://payload.lockbox.api.cloud.yandex.net/lockbox/v1/secrets

export SERVICE_ACCOUNT=robot-service
export MAIN_USER=andre487

export CONTAINER_NAME=sms487-api
export DOCKER_IMAGE=cr.yandex/crp998oqenr95rs4gf9a/sms487-api:latest

get_main_user_public_key() {
    local secret_file
    local ssh_key_secret
    local payload

    ssh_key_secret=e6q6vfo2vo565h0cb0nq
    secret_file="$SECRET_DIR/main_user_id_rsa.pub"

    if [[ ! -f "$secret_file" ]]; then
        payload="$(curl -s --fail -H "Authorization: Bearer $IAM_TOKEN" "$LOCKBOX_SECRET_URL/$ssh_key_secret/payload")"
        jq <<< "$payload" -r '.entries | map(select(.key == "id_rsa.pub")) | .[0].textValue' >"$secret_file"
    fi

    echo "$secret_file"
}
