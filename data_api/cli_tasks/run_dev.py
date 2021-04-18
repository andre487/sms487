from . import common


def run(c, port, rebuild_venv, clear_db):
    common.prepare_virtual_env(c, rebuild_venv)
    proc, _ = common.start_dev_instance(port, force_db_cleaning=clear_db)

    proc.wait()
    if proc.returncode:
        raise RuntimeError(f'Process return non-zero status {proc.returncode}')
