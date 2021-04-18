import cli_tasks
from cli_tasks import common
from invoke import task


@task
def run_dev(c, port=8080, rebuild_venv=False, clear_db=False):
    """Run Flask dev server"""
    cli_tasks.run_dev.run(c, port, rebuild_venv, clear_db)


@task
def lint(c, rebuild_venv=False):
    """Run flake8"""
    cli_tasks.run_linters.run(c, rebuild_venv)


@task
def install(c, rebuild_venv=False, packages=''):
    """Install packages: invoke install --packages='flask pytest'"""
    cli_tasks.install.run(c, rebuild_venv, packages)


@task
def freeze(c, rebuild_venv=False):
    """Freeze requirements.txt"""
    cli_tasks.freeze_requirements.run(c, rebuild_venv)


@task
def http_test(c, rebuild_venv=False, k=None):
    """Run HTTP handlers test on dev instance"""
    cli_tasks.http_test.run(c, rebuild_venv, test_filter=k)


@task
def docker_build(c):
    """Build app Docker image"""
    cli_tasks.docker_build.run(c)


@task
def docker_push(c):
    """Push app Docker image to registry"""
    cli_tasks.docker_push.run(c)


@task
def docker_run(c, port=8181):
    """Run app in Docker container"""
    cli_tasks.docker_run.run(c, port)


@task
def docker_test(c, rebuild_venv=False):
    """Run HTTP handlers test on Docker instance"""
    cli_tasks.docker_test.run(c, rebuild_venv)


@task
def prepare_secrets(c, rebuild_venv=False, no_secret_cache=False):
    """Prepare secrets for production"""
    cli_tasks.prepare_secrets.run(c, rebuild_venv, no_secret_cache)


@task
def create_local_venv(c, rebuild_venv=False):
    """Prepare .venv dir for using in IDE"""
    common.prepare_virtual_env(c, rebuild_venv)


@task
def make_deploy(c, rebuild_venv=False, no_secret_cache=False):
    """Deploy current work dir to production"""
    cli_tasks.run_linters.run(c, rebuild_venv)

    cli_tasks.prepare_secrets.run(c, rebuild_venv, no_secret_cache)

    cli_tasks.docker_build.run(c)
    cli_tasks.docker_test.run(c, rebuild_venv)
    cli_tasks.docker_push.run(c)

    c.run(
        f'ansible-playbook '
        f'{common.PROJECT_DIR}/deploy/setup.yml'
    )
