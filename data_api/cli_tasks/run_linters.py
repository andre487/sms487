import logging
from . import common


def run(c):
    common.prepare_virtual_env(c)

    logging.info('Running linters')
    c.run(f'{common.PYTHON} -m flake8')
