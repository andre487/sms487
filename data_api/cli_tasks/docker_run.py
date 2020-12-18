import time
from . import common


def run(_, port):
    common.start_docker_instance(port)
    time.sleep(int(1e6))
