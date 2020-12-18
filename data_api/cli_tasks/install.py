from . import common


def run(c, recreate_venv, packages):
    common.prepare_virtual_env(c, recreate_venv)
    c.run(f'{common.PYTHON} -m pip install -U {packages}')
