from . import common


def run(c, packages):
    common.prepare_virtual_env(c)
    c.run(f'{common.PYTHON} -m pip install -U {packages}')
