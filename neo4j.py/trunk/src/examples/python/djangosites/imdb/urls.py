from django.conf.urls.defaults import *

urlpatterns = patterns('djangosites.imdb.views',
    url(r'^$', 'index'),
    url(r'^search', 'search'),
    url(r'^movie/$', 'list_movies'),
)
