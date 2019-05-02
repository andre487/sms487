#@IgnoreInspection BashAddShebang
export MONGO_DB_NAME='sms487_test'
export AUTH_MONGO_DB_NAME='sms487_test'

export FLASK_APP=api.py
export FLASK_ENV=dev
export FLASK_DEBUG=1

if [[ -z "$NO_PUBLIC_KEY_FILE" ]]; then
    export AUTH_PUBLIC_KEY_FILE="$HOME/.private/auth487/public_key.pem"
fi
export AUTH_DOMAIN="https://auth.andre.life"

if [[ -n "$(lsof -i :5487)" ]]; then
    export AUTH_DOMAIN="http://localhost:5487"
fi

echo "Using auth domain $AUTH_DOMAIN"

dev_env="$HOME/.venv/sms487-server"
