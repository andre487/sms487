# SMS487

SMS handler for Android. Sends messages from phone to server
for seeng them on PC.

## Android client

Android client located in `client` dir. Requires Android 8.

## Data API

Flask server located in `data_api` dir. It uses Python 3.

For working needs environment vars:

  * `SMS_USER_NAME` – user name for auth
  * `SMS_USER_KEY` – SHA256 hex digest of password for auth
