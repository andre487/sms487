import os
import subprocess
from . import common


def run(_):
    os.chdir(common.PROJECT_DIR)

    docker = common.get_docker()
    subprocess.check_call((
        docker, 'build',
        '-t', common.DOCKER_IMAGE_NAME,
        '--force-rm', '.',
    ))
