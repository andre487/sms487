#!/usr/bin/env bash
set -e -o pipefail

proj_dir="$(cd "$(dirname "$0")" && pwd)"
cd "$proj_dir"

cpu_count="$(getconf _NPROCESSORS_ONLN)"
worker_count="$(($cpu_count * 2))"

uwsgi --http '0.0.0.0:5000' \
    --workers "$worker_count" \
    --master \
    --disable-logging \
    --no-orphans \
    --wsgi-file api.py \
    --callable app \
    --static-map /static="$proj_dir/static"
