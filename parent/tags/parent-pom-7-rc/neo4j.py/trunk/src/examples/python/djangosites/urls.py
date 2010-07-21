# -*- coding: utf-8 -*-

# Copyright (c) 2008-2010 "Neo Technology,"
#     Network Engine for Objects in Lund AB [http://neotechnology.com]
# 
# This file is part of Neo4j.py.
# 
# Neo4j.py is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
    app('blog/', "Blog Example", 'djangosites.blog.urls'),

    # Admin interface
    (r'^admin/', include(admin.site.urls)),
    #(r'^admin/doc/', include('django.contrib.admindocs.urls')),
)
