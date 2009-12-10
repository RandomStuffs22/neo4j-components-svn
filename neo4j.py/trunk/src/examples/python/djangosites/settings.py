import os, sys

MODEL_TYPE = os.environ.get('DJANGOSITES_MODEL_TYPE', 'neo4j')

_site_path = os.path.split(os.path.abspath(__file__))[0]
_data_path = os.path.join(_site_path, 'data')
if not os.path.exists(_data_path):
    os.mkdir(_data_path)

# Django settings for djangosites project.

DEBUG = True
TEMPLATE_DEBUG = DEBUG

ADMINS = (
    ('Tobias Ivarsson', 'tobias@neotechnology.com'),
)

MANAGERS = ADMINS

NEO4J_RESOURCE_URI = os.path.join(_data_path, 'neo')
NEO4J_OPTIONS = {}

DATABASE_ENGINE = ''           # 'postgresql_psycopg2', 'postgresql', 'mysql', 'sqlite3' or 'oracle'.
if 'java' in sys.platform.lower(): # use postgres with jython
    DATABASE_ENGINE = 'doj.backends.zxjdbc.postgresql'
    DATABASE_NAME = "djangosites"
else: # use sqlite with cpython
    DATABASE_ENGINE = 'sqlite3'
    DATABASE_NAME = os.path.join(_data_path, 'sqlite')
DATABASE_USER = 'django'             # Not used with sqlite3.
DATABASE_PASSWORD = 'jython'         # Not used with sqlite3.
DATABASE_HOST = ''             # Set to empty string for localhost. Not used with sqlite3.
DATABASE_PORT = ''             # Set to empty string for default. Not used with sqlite3.

# Local time zone for this installation. Choices can be found here:
# http://en.wikipedia.org/wiki/List_of_tz_zones_by_name
# although not all choices may be available on all operating systems.
# If running in a Windows environment this must be set to the same as your
# system time zone.
TIME_ZONE = 'Europe/Stockholm'

# Language code for this installation. All choices can be found here:
# http://www.i18nguy.com/unicode/language-identifiers.html
LANGUAGE_CODE = 'en-us'

SITE_ID = 1

# If you set this to False, Django will make some optimizations so as not
# to load the internationalization machinery.
USE_I18N = True

# Absolute path to the directory that holds media.
# Example: "/home/media/media.lawrence.com/"
MEDIA_ROOT = ''

# URL that handles the media served from MEDIA_ROOT. Make sure to use a
# trailing slash if there is a path component (optional in other cases).
# Examples: "http://media.lawrence.com", "http://example.com/media/"
MEDIA_URL = ''

# URL prefix for admin media -- CSS, JavaScript and images. Make sure to use a
# trailing slash.
# Examples: "http://foo.com/media/", "/media/".
ADMIN_MEDIA_PREFIX = '/media/'

# Make this unique, and don't share it with anybody.
SECRET_KEY = 'hp%z_4y%u&8c#m5l$_d#%4benu8f%n*v0rf5ck#_&0h+w&0j*w'

# List of callables that know how to import templates from various sources.
TEMPLATE_LOADERS = (
    'django.template.loaders.filesystem.load_template_source',
    'django.template.loaders.app_directories.load_template_source',
#     'django.template.loaders.eggs.load_template_source',
)

MIDDLEWARE_CLASSES = (
    'django.middleware.common.CommonMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
)

ROOT_URLCONF = 'djangosites.urls'

TEMPLATE_DIRS = (
    '/'.join(_site_path.split(os.path.sep) + ['templates']),
)

INSTALLED_APPS = (
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.sites',
    'django.contrib.admin',
    # configure installed apps
    'djangosites.imdb',
    'djangosites.faces',
)
