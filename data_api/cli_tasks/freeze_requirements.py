from . import common


def run(c, rebuild_venv):
    common.prepare_virtual_env(c, rebuild_venv)
    c.run(f'{common.PYTHON} -m pip freeze > {common.PROJECT_DIR}/requirements.txt')
    print('OK')
