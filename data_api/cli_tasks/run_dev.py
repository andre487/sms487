import os
import subprocess
from . import common


def run(c, port, recreate_venv):
    common.prepare_virtual_env(c, recreate_venv)
    mongo_port = common.run_mongo()

    env = common.DEFAULT_APP_ENV.copy()
    env['AUTH_MONGO_DB_NAME'] = env['MONGO_DB_NAME'] = common.DEV_DB_NAME
    env.update(os.environ)
    env['MONGO_PORT'] = mongo_port

    p = subprocess.Popen(
        (common.PYTHON, '-m', 'flask', 'run', '-p', str(port)),
        cwd=common.PROJECT_DIR,
        env=env,
    )

    p.wait()
    if p.returncode:
        raise RuntimeError(f'Process return non-zero status {p.returncode}')
