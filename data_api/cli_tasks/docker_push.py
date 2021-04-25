import os
import subprocess
from . import common


def run(_, tag='latest'):
    os.chdir(common.PROJECT_DIR)

    docker = common.get_docker()
    subprocess.check_call((docker, 'push', common.DOCKER_IMAGE_NAME + ':' + tag))

    if tag != 'latest':
        subprocess.check_call((docker, 'push', common.DOCKER_IMAGE_NAME + ':latest'))
