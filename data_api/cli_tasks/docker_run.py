from . import common


def run(_, port):
    proc = common.start_docker_instance(port)

    proc.wait()
    if proc.returncode:
        raise RuntimeError(f'Process return non-zero status {proc.returncode}')

