#!/usr/bin/env bash
set -euo pipefail

zone="${1:-ru-central1-c}"
echo "Images and containers in zone $zone"

cur_dir="$(cd "$(dirname "$0")" && pwd)"
source "$cur_dir/common.sh"

ip_addr="$(get_prod_machine "$zone")"
ssh -o "StrictHostKeyChecking no" -i ~/.ssh/id_rsa_cloud "yc-user@$ip_addr" 'docker images; docker ps'
