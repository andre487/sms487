FROM ubuntu:focal

ADD ./requirements.txt /usr/local/bin/app/requirements.txt

RUN set -x && \
    apt-get update && \
    apt-get install -y python3 python3-pip build-essential libssl-dev libffi-dev python3-dev git curl && \
    python3 -m pip install --upgrade --no-cache-dir pip && \
    python3 -m pip install --no-cache-dir -r /usr/local/bin/app/requirements.txt && \
    mkdir -p /usr/local/share/ca-certificates/Yandex && \
    curl "https://storage.yandexcloud.net/cloud-certs/CA.pem" -o /usr/local/share/ca-certificates/Yandex/YandexInternalRootCA.crt && \
    apt-get purge -y python3-pip build-essential git curl && \
    apt-get clean -y && \
    apt-get autoclean -y && \
    rm -f /var/cache/apt/*.bin && \
    find /tmp -type f -delete && \
    find /var/tmp -type f -delete

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

ADD . /usr/local/bin/app
CMD /usr/local/bin/app/entry-point.sh

EXPOSE 5000
