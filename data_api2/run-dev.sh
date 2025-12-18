#!/usr/bin/env bash
set -eufo pipefail

cd "$(dirname "$0")"

export HTTP_USER=user
export HTTP_PASSWORD=password
SQS_QUEUE_URL="$(cat dev-secrets/sqs_queue_url)"; export SQS_QUEUE_URL
SQS_ACCESS_KEY="$(cat dev-secrets/sqs_access_key)"; export SQS_ACCESS_KEY
SQS_SECRET_KEY="$(cat dev-secrets/sqs_secret_key)"; export SQS_SECRET_KEY
TIME_FORMAT="$(cat dev-secrets/time_format)"; export TIME_FORMAT

go run .
