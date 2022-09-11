#!/usr/bin/env bash
set -eufo pipefail

cur_dir="$(cd "$(dirname "$0")" && pwd)"
project_dir="$cur_dir/.."

cd "$project_dir"
python3 -m venv --upgrade-deps --clear ./venv
./venv/bin/pip install -r requirements.txt
