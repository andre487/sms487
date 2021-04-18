import logging
import subprocess
import time
from . import common


def run(c, rebuild_venv):
    common.prepare_virtual_env(c, rebuild_venv)

    port = common.get_free_port()
    logging.info('Using port %s', port)

    logging.info('Start Docker instance')
    _, mongo_port = common.start_docker_instance(port, db_name=common.TEST_DB_NAME)
    time.sleep(2)

    test_proc = subprocess.Popen((common.PYTHON, '-m', 'pytest', '-s', 'http_test.py'), env={
        'APP_PORT': str(port),
        'AUTH_DEV_MODE': '1',
        'AUTH_MONGO_DB_NAME': common.TEST_DB_NAME,
        'MONGO_DB_NAME': common.TEST_DB_NAME,
        'MONGO_PORT': mongo_port,
    }, cwd=common.PROJECT_DIR)
    test_proc.wait()

    common.drop_db(common.TEST_DB_NAME)

    if test_proc.returncode:
        raise RuntimeError(f'Test failed: {test_proc.returncode}')
