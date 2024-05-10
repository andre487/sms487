import os
import subprocess
from . import common


def run(_, tag='latest'):
    os.chdir(common.PROJECT_DIR)

    docker = common.get_docker()
    subprocess.check_call((docker, 'build', '-t', common.DOCKER_IMAGE_NAME + ':' + tag, '--force-rm', '.',))

    if tag != 'latest':
        subprocess.check_call((
            docker, 'tag',
            common.DOCKER_IMAGE_NAME + ':' + tag,
            common.DOCKER_IMAGE_NAME + ':latest',
        ))
