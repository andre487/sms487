import logging
import os

cur_dir = os.path.dirname(__file__)


def setup_filters(app):
    @app.template_filter('static_version')
    def static_version_filter(file_name):
        full_path = os.path.join(cur_dir, file_name[1:])

        try:
            version = int(os.path.getmtime(full_path))
        except OSError as e:
            logging.warning('Static version error: %s', e)
            version = 0

        return '{0}?v=0x{1}'.format(file_name, version)
