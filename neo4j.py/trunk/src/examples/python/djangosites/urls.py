from django.conf import settings
from django.conf.urls.defaults import *

from django.contrib import admin
admin.autodiscover()

def disabled(title):
    from django.http import HttpResponse
    def disabled(request):
        return HttpResponse('"%s" is disabled.' % (title,))
    return disabled

def app():
    from django.shortcuts import render_to_response
    global app, applist
    links = []
    def app(url, title, urls, when=lambda:True):
        links.append(('/' + url, title))
        return '^' + url, include(urls) if when() else disabled(title)
    def applist(request):
        return render_to_response('list_of_links.html', {
                'title': "Available applications",
                'links': links,
                })
    return '^$', applist

urlpatterns = patterns('',
    app(),
    # Apps
    app('imdb/', "IMDB Example", 'djangosites.imdb.urls'),
    app('faces/', "Faces Example", 'djangosites.faces.urls',
        when=lambda:settings.MODEL_TYPE == 'neo4j'),

    # Admin interface
    (r'^admin/', include(admin.site.urls)),
    #(r'^admin/doc/', include('django.contrib.admindocs.urls')),
)
