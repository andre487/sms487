#!/usr/bin/env bash
set -euo pipefail

zone="${1:-ru-central1-d}"
echo "SSH to container in zone $zone"

cur_dir="$(cd "$(dirname "$0")" && pwd)"
source "$cur_dir/common.sh"

ip_addr="$(get_prod_machine "$zone")"
ssh -tt -o "StrictHostKeyChecking accept-new" "yc-user@$ip_addr" 'docker exec -ti "$(docker ps | grep sms487-api | cut -d " " -f1)" bash'
