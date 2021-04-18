from . import common


def run(c, rebuild_venv, packages):
    common.prepare_virtual_env(c, rebuild_venv)
    c.run(f'{common.PYTHON} -m pip install -U {packages}')
