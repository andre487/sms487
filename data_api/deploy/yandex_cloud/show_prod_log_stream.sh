#!/usr/bin/env bash
set -euo pipefail

zone="${1:-ru-central1-c}"
echo "Log stream for container in zone $zone"

cur_dir="$(cd "$(dirname "$0")" && pwd)"
source "$cur_dir/common.sh"

ip_addr="$(get_prod_machine "$zone")"
ssh -o "StrictHostKeyChecking no" "yc-user@$ip_addr" 'docker logs -f "$(docker ps | grep sms487-api | cut -d " " -f1)"'
