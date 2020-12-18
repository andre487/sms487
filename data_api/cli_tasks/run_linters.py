import logging
from . import common


def run(c, recreate_venv):
    common.prepare_virtual_env(c, recreate_venv=recreate_venv)

    logging.info('Running linters')
    c.run(f'{common.PYTHON} -m flake8')
