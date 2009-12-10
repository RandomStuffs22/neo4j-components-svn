from django.conf import settings

if settings.MODEL_TYPE == 'neo4j':
    from djangosites.imdb.neo_model import *
else:
    from djangosites.imdb.orm_model import *
