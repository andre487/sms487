import atexit
import json
import logging
import os
import shutil
import socket
import subprocess

logging.basicConfig(level=logging.INFO)

PROJECT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
VENV_DIR = os.path.join(PROJECT_DIR, '.venv')
PYTHON = os.path.join(VENV_DIR, 'bin', 'python')
TEST_DATA_DIR = os.path.join(PROJECT_DIR, 'test_data')

DOCKER_MONGO_NAME = 'sms487-mongo'
DEV_DB_NAME = 'sms487'
TEST_DB_NAME = 'sms487_test'

DEFAULT_APP_ENV = {
    'MONGO_DB_NAME': TEST_DB_NAME,
    'AUTH_MONGO_DB_NAME': TEST_DB_NAME,
    'FLASK_APP': 'api.py',
    'FLASK_ENV': 'dev',
    'FLASK_DEBUG': '1',
    'AUTH_DOMAIN': 'https://auth.andre.life',
    'AUTH_PRIVATE_KEY_FILE': os.path.join(TEST_DATA_DIR, 'auth_keys', 'key'),
    'AUTH_PUBLIC_KEY_FILE': os.path.join(TEST_DATA_DIR, 'auth_keys', 'key.pub.pem'),
    'ENABLE_TEST_TOKEN_SET': '1',
}


def prepare_virtual_env(c, recreate_venv):
    os.chdir(PROJECT_DIR)

    if os.path.exists(VENV_DIR):
        if recreate_venv:
            shutil.rmtree(VENV_DIR)
        else:
            return

    c.run(f'python3 -m virtualenv --always-copy --download {VENV_DIR}')
    c.run(f'{PYTHON} -m pip install --upgrade pip')
    c.run(f'{PYTHON} -m pip install -r {PROJECT_DIR}/requirements.txt')


def start_dev_instance(port, db_name=DEV_DB_NAME, force_db_cleaning=False):
    mongo_port = run_mongo(force_db_cleaning=force_db_cleaning, db_name=db_name)

    env = DEFAULT_APP_ENV.copy()
    env['AUTH_MONGO_DB_NAME'] = env['MONGO_DB_NAME'] = db_name
    env.update(os.environ)
    env['MONGO_PORT'] = mongo_port

    return subprocess.Popen(
        (PYTHON, '-m', 'flask', 'run', '-p', str(port)),
        cwd=PROJECT_DIR,
        env=env,
    )


def run_mongo(force_db_cleaning=False, db_name=DEV_DB_NAME):
    logging.info('Start MongoDB using Docker')

    docker = get_docker()
    cont_id, is_running = get_mongo_container_data(docker)
    if not cont_id:
        subprocess.check_output((docker, 'run', '-d', '-P', '--name', DOCKER_MONGO_NAME, 'mongo'))
        cont_id, is_running = get_mongo_container_data(docker)

    if not is_running:
        subprocess.check_output((docker, 'start', cont_id))

    mongo_port = get_mongo_port(docker, cont_id)
    fill_db_with_fixture(mongo_port, db_name, force=force_db_cleaning)

    atexit.register(stop_mongo)

    return mongo_port


def stop_mongo():
    docker = get_docker()
    cont_id, is_running = get_mongo_container_data(docker)

    if is_running:
        logging.info('Stopping MongoDB')
        subprocess.check_output((docker, 'stop', cont_id))


def get_docker():
    return subprocess.check_output(('which', 'docker')).strip()


def get_mongo_container_data(docker):
    out = str(subprocess.check_output((docker, 'ps', '-a')).strip())
    lines = out.split(r'\n')

    cont_id = None
    is_running = None

    mongo_line = None
    for line in lines:
        if DOCKER_MONGO_NAME in line:
            mongo_line = line
            break

    if not mongo_line:
        return cont_id, is_running

    cont_id, _ = mongo_line.strip().split(' ', 1)
    info = subprocess.check_output((docker, 'inspect', cont_id))

    info_data = json.loads(info)
    if not info_data:
        raise RuntimeError('Empty docker inspect info')

    is_running = info_data[0]['State']['Running']

    return cont_id, is_running


def get_mongo_port(docker, cont_id):
    info = subprocess.check_output((docker, 'inspect', cont_id))

    info_data = json.loads(info)
    if not info_data:
        raise RuntimeError('Empty docker inspect info')

    port_data = info_data[0]['NetworkSettings']['Ports'].get('27017/tcp')
    if not port_data:
        raise RuntimeError('No Mongo port 27017 exposed')

    return port_data[0]['HostPort']


def fill_db_with_fixture(mongo_port, db_name, force=False):
    script_path = os.path.join(TEST_DATA_DIR, 'manage-db.py')

    call_args = [PYTHON, script_path, 'setup']
    if force:
        call_args.append('--force')

    subprocess.check_call(call_args, env={
        'MONGO_PORT': mongo_port,
        'MONGO_DB_NAME': db_name,
        'AUTH_MONGO_DB_NAME': db_name,
    })


def drop_db(db_name):
    logging.info('Drop DB %s', db_name)

    docker = get_docker()
    cont_id, is_running = get_mongo_container_data(docker)
    if not is_running:
        raise RuntimeError('Mongo container is not running')

    mongo_port = get_mongo_port(docker, cont_id)
    script_path = os.path.join(TEST_DATA_DIR, 'manage-db.py')

    subprocess.check_call((PYTHON, script_path, 'tear-down'), env={
        'MONGO_PORT': mongo_port,
        'MONGO_DB_NAME': db_name,
        'AUTH_MONGO_DB_NAME': db_name,
    })


def get_auth_token():
    token_file = os.path.join(TEST_DATA_DIR, 'test-auth-token.txt')
    with open(token_file) as fp:
        return fp.read().strip()


def get_free_port():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.bind(('0.0.0.0', 0))
    return sock.getsockname()[1]
