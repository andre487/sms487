import atexit
import json
import logging
import os
import shutil
import socket
import subprocess
from functools import partial

logging.basicConfig(level=logging.INFO)

PROJECT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
VENV_DIR = os.path.join(PROJECT_DIR, '.venv')
PYTHON = os.path.join(VENV_DIR, 'bin', 'python')
TEST_DATA_DIR = os.path.join(PROJECT_DIR, 'test_data')
SECRET_DIR = os.path.join(PROJECT_DIR, '.secret')

DOCKER_IMAGE_NAME = 'cr.yandex/crp998oqenr95rs4gf9a/sms487-api'
DOCKER_MONGO_NAME = 'sms487-mongo'
DOCKER_APP_NAME = 'sms487-server-test'
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


def prepare_virtual_env(c, rebuild_venv):
    os.chdir(PROJECT_DIR)

    if os.path.exists(VENV_DIR):
        if rebuild_venv:
            shutil.rmtree(VENV_DIR)
        else:
            return

    c.run(f'python3 -m venv --copies {VENV_DIR}')
    c.run(f'{PYTHON} -m pip install --upgrade pip')
    c.run(f'{PYTHON} -m pip install -r {PROJECT_DIR}/requirements.txt')


def start_dev_instance(port, db_name=DEV_DB_NAME, force_db_cleaning=False):
    mongo_port = run_mongo(force_db_cleaning=force_db_cleaning, db_name=db_name)

    env = DEFAULT_APP_ENV.copy()
    env['DEPLOY_TYPE'] = 'dev'
    env['AUTH_MONGO_DB_NAME'] = env['MONGO_DB_NAME'] = db_name
    env.update(os.environ)
    env['MONGO_PORT'] = mongo_port
    env['AUTH_DEV_MODE'] = '1'

    return subprocess.Popen(
        (PYTHON, '-m', 'flask', 'run', '-p', str(port)),
        cwd=PROJECT_DIR,
        env=env,
    ), mongo_port


def start_docker_instance(port, tag='latest', db_name=DEV_DB_NAME, force_db_cleaning=False):
    logging.info('Starting Docker app instance')
    mongo_port = run_mongo(force_db_cleaning=force_db_cleaning, db_name=db_name)
    mongo_cont_name = DOCKER_MONGO_NAME + '-' + db_name

    docker = get_docker()
    cont_id, _ = get_container_data(docker, DOCKER_APP_NAME)
    if cont_id:
        subprocess.check_call((docker, 'rm', '-f', cont_id))

    cont_id = subprocess.check_output((
        docker, 'run', '--rm', '-d', '--name', DOCKER_APP_NAME,
        '--link', mongo_cont_name,
        '-p', f'127.0.0.1:{port}:5000',
        '-v', f'{os.path.join(TEST_DATA_DIR, "auth_keys")}:/opt/auth_keys',
        '-e', 'DEPLOY_TYPE=dev',
        '-e', 'AUTH_PRIVATE_KEY_FILE=/opt/auth_keys/key',
        '-e', 'AUTH_PUBLIC_KEY_FILE=/opt/auth_keys/key.pub.pem',
        '-e', 'FLASK_APP=app.py',
        '-e', 'FLASK_ENV=dev',
        '-e', 'FLASK_DEBUG=1',
        '-e', 'AUTH_DEV_MODE=1',
        '-e', f'MONGO_HOST={mongo_cont_name}',
        '-e', f'MONGO_DB_NAME={db_name}',
        '-e', f'AUTH_MONGO_DB_NAME={db_name}',
        DOCKER_IMAGE_NAME + ':' + tag,
    )).strip()

    atexit.register(partial(remove_docker_container, cont_id))
    atexit.register(partial(get_docker_instance_logs, cont_id))

    return cont_id, mongo_port


def remove_docker_container(cont_id):
    logging.info('Removing Docker container %s', cont_id)
    docker = get_docker()
    subprocess.check_call((docker, 'rm', '-f', cont_id))


def get_docker_instance_logs(cont_id):
    docker = get_docker()
    if os.getenv('SHOW_LOGS') != '0':
        subprocess.check_call((docker, 'logs', cont_id))


def run_mongo(force_db_cleaning=False, db_name=DEV_DB_NAME):
    logging.info('Start MongoDB using Docker')

    docker = get_docker()
    container_name = DOCKER_MONGO_NAME + '-' + db_name

    cont_id, is_running = get_container_data(docker, container_name)
    if not cont_id:
        subprocess.check_output((docker, 'run', '-d', '-P', '--name', container_name, 'mongo'))
        cont_id, is_running = get_container_data(docker, container_name)

    if not is_running:
        subprocess.check_output((docker, 'start', cont_id))

    mongo_port = get_container_service_port(docker, cont_id, '27017/tcp')
    fill_db_with_fixture(mongo_port, db_name, force=force_db_cleaning)

    atexit.register(stop_mongo)

    return mongo_port


def stop_mongo():
    docker = get_docker()
    cont_id, is_running = get_container_data(docker, DOCKER_MONGO_NAME)

    if is_running:
        logging.info('Stopping MongoDB')
        subprocess.check_output((docker, 'stop', cont_id))


def get_docker():
    return subprocess.check_output(('which', 'docker')).strip()


def get_container_data(docker, container_name):
    out = str(subprocess.check_output((docker, 'ps', '-a')).strip())
    lines = out.split(r'\n')

    cont_id = None
    is_running = None

    info_line = None
    for line in lines:
        if container_name in line:
            info_line = line
            break

    if not info_line:
        return cont_id, is_running

    cont_id, _ = info_line.strip().split(' ', 1)
    info = subprocess.check_output((docker, 'inspect', cont_id))

    info_data = json.loads(info)
    if not info_data:
        raise RuntimeError('Empty docker inspect info')

    is_running = info_data[0]['State']['Running']

    return cont_id, is_running


def get_container_service_port(docker, cont_id, internal_port):
    info = subprocess.check_output((docker, 'inspect', cont_id))

    info_data = json.loads(info)
    if not info_data:
        raise RuntimeError('Empty docker inspect info')

    port_data = info_data[0]['NetworkSettings']['Ports'].get(internal_port)
    if not port_data:
        raise RuntimeError(f'No port {internal_port} exposed')

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
    cont_id, is_running = get_container_data(docker, DOCKER_MONGO_NAME)
    if not is_running:
        raise RuntimeError('Mongo container is not running')

    mongo_port = get_container_service_port(docker, cont_id, '27017/tcp')
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


def get_csrf_token():
    token_file = os.path.join(TEST_DATA_DIR, 'test-csrf-token.txt')
    with open(token_file) as fp:
        return fp.read().strip()
