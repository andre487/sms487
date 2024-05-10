from . import common


def run(c, port, clear_db):
    common.prepare_virtual_env(c)
    proc, _ = common.start_dev_instance(port, force_db_cleaning=clear_db)

    proc.wait()
    if proc.returncode:
        raise RuntimeError(f'Process return non-zero status {proc.returncode}')
