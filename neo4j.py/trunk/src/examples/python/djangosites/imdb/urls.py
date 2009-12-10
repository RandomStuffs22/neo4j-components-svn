from django.conf.urls.defaults import *

urlpatterns = patterns('djangosites.imdb.views',
    url(r'^$', 'index'),
    url(r'^search', 'search'),
    url(r'^movie/$', 'list_movies'),
    url(r'^movie/(?P<movie>\d+)/$', 'view_movie'),
    url(r'^actor/$', 'list_actors'),
    url(r'^actor/(?P<actor>\d+)/$', 'view_actor'),
    url(r'^role/(?P<role>\d+)/$', 'view_role'),
)
