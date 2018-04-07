FROM ubuntu:bionic

ADD ./requirements.txt /usr/local/bin/app/requirements.txt

RUN set -x && \
    apt-get update && \
    apt-get install -y python3-pip && \
    pip3 install --no-cache-dir -r /usr/local/bin/app/requirements.txt && \
    apt-get purge -y python3-pip && \
    apt-get autoremove -y && \
    apt-get install -y python3 && \
    apt-get clean -y && \
    apt-get autoclean -y && \
    rm -f /var/cache/apt/*.bin && \
    find /tmp -type f -delete && \
    find /var/tmp -type f -delete

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

ADD . /usr/local/bin/app
CMD FLASK_APP=/usr/local/bin/app/api.py flask run --host=0.0.0.0

EXPOSE 5000
