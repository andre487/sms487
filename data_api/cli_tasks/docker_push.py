import os
import subprocess
from . import common


def run(_, tag='latest'):
    os.chdir(common.PROJECT_DIR)

    docker = common.get_docker()
    print('Docker push with tag', tag)
    subprocess.check_call((docker, 'push', common.DOCKER_IMAGE_NAME + ':' + tag))

    if tag != 'latest':
        print('Docker push with tag latest')
        subprocess.check_call((docker, 'push', common.DOCKER_IMAGE_NAME + ':latest'))
