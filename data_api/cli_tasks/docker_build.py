import os
import subprocess
from . import common


def run(_):
    os.chdir(common.PROJECT_DIR)

    docker = common.get_docker()
    subprocess.check_call((
        docker, 'build',
        '-t', 'andre487/sms487-api:latest',
        '--force-rm', '.',
    ))
