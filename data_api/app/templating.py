import logging
import os
import re
from flask import Markup

LINK_PATTERN = re.compile(r'(https?://[\w.:/#?&@~=%!+-]+)([^\w.:/#?&@~=%!+-]|$)')
LINK_REPLACEMENT = r'<a class="link" href="\1" target="_blank" rel="noopener">\1</a>\2'
EOL_PATTERN = re.compile(r'\s*[\r\n]+\s*')
EOL_REPLACEMENT = r'<br>'
CUR_DIR = os.path.dirname(__file__)


def setup_filters(app):
    @app.template_filter('static_version')
    def static_version_filter(file_name):
        full_path = os.path.join(CUR_DIR, '..', file_name[1:])

        version = 0
        try:
            version = int(os.path.getmtime(full_path))
        except OSError as e:
            logging.warning('Static version error: %s', e)

        return '{0}?v=0x{1}'.format(file_name, version)

    @app.template_filter('handle_tags')
    def handle_tags(text):
        if not text:
            return text

        result = Markup(text).striptags()
        result = LINK_PATTERN.sub(LINK_REPLACEMENT, result)
        result = EOL_PATTERN.sub(EOL_REPLACEMENT, result)

        return result
