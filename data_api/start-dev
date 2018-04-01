#!/usr/bin/env bash

set -e -o pipefail

export SMS_USER_NAME="$(cat "$HOME/.private/SMS_USER_NAME")"
export SMS_USER_KEY="$(cat "$HOME/.private/SMS_USER_KEY")"

export FLASK_DEBUG=1
export FLASK_APP=api.py

flask run
