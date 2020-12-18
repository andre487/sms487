from . import common


def run(c, port, recreate_venv):
    common.prepare_virtual_env(c, recreate_venv)
    proc = common.start_dev_instance(port)

    proc.wait()
    if proc.returncode:
        raise RuntimeError(f'Process return non-zero status {proc.returncode}')
