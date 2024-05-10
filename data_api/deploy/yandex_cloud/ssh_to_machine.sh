#!/usr/bin/env bash
set -euo pipefail

zone="${1:-ru-central1-d}"
echo "SSH to machine in zone $zone"

cur_dir="$(cd "$(dirname "$0")" && pwd)"
source "$cur_dir/common.sh"

ip_addr="$(get_prod_machine "$zone")"
ssh -tt -o "StrictHostKeyChecking no" "yc-user@$ip_addr"
