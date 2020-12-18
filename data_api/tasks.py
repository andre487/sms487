import cli_tasks
from invoke import task


@task
def run_dev(c, port=8080, recreate_venv=False):
    """Run Flask dev server"""
    cli_tasks.run_dev.run(c, port, recreate_venv)


@task
def install(c, recreate_venv=False, packages=''):
    """Install packages: invoke install --packages='flask pytest'"""
    cli_tasks.install.run(c, recreate_venv, packages)


@task
def freeze(c, recreate_venv=False):
    """Freeze requirements.txt"""
    cli_tasks.freeze_requirements.run(c, recreate_venv)


@task
def http_test(c, recreate_venv=False):
    """Run HTTP handlers test"""
    cli_tasks.http_test.run(c, recreate_venv)
