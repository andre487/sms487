import logging
from . import common


def run(c, rebuild_venv):
    common.prepare_virtual_env(c, rebuild_venv=rebuild_venv)

    logging.info('Running linters')
    c.run(f'{common.PYTHON} -m flake8')
