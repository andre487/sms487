#!/usr/bin/env bash
set -ex -o pipefail

dir="$(cd "$(dirname "$0")" && pwd)"
project_dir="$dir/.."

cd "$project_dir"

port="${1:-8080}"

curl -v --data "message_type=sms&device_id=test&tel=01&date_time=2020-01-01%2000:00&sms_date_time=2020-01-01%2000:00&text=SMSTest" \
  -H "Cookie: Dev-Auth-Token=$(cat test_data/test-auth-token.txt)" \
  "http://127.0.0.1:$port/add-sms"
