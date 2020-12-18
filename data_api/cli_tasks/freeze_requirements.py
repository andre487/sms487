from . import common


def run(c, recreate_venv):
    common.prepare_virtual_env(c, recreate_venv)
    c.run(f'{common.PYTHON} -m pip freeze > {common.PROJECT_DIR}/requirements.txt')
    print('OK')
