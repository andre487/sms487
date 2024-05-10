#!/usr/bin/env bash
set -euo pipefail

zone="${1:-ru-central1-d}"
echo "Images and containers in zone $zone"

cur_dir="$(cd "$(dirname "$0")" && pwd)"
source "$cur_dir/common.sh"

ip_addr="$(get_prod_machine "$zone")"
ssh -i "$(get_main_user_private_key)" -o "StrictHostKeyChecking no" "yc-user@$ip_addr" 'docker images; docker ps'
