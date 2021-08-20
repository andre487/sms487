#!/usr/bin/env bash
set -exufo pipefail

cd "$(dirname "$0")/.."
proj_dir="$(pwd)"

if [[ ! -d "venv" ]] || [[ "${REBUILD_VENV:-0}" == 1 ]]; then
    rm -rf venv
    python3 -m venv venv
    ./venv/bin/python3 -m pip install -r requirements.txt
fi

source ./venv/bin/activate

export FLASK_APP=api.py
export ENABLE_TEST_TOKEN_SET=1
export FLASK_ENV=development

flask run --host 0.0.0.0
