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

from __future__ import with_statement

from django.shortcuts import render_to_response
from django.http import HttpResponse, HttpResponseRedirect, Http404

from djangosites.blog import models

import time, functools

from neo4j.model.django_model import NeoServiceProperty
NeoServiceProperty = NeoServiceProperty()
def getdb():
    return NeoServiceProperty.__get__(None)

def transactional(func):
    @functools.wraps(func)
    def transactional(*args,**kwargs):
        return func(*args,**kwargs)
    return transactional

def index(request):
    if request.method == 'POST': return create_user(request)
    result = ['<h2>Users</h2>']
    for user in models.User.objects.all():
        result.append('<a href="/blog/~%s">%s</a><br/>' % (
                user.username, user.name))
    result.append('<h2>Blogs</h2>')
    for blog in models.Blog.objects.all():
        result.append('<a href="/blog/%s">%s</a><br/>' % (
                blog.identifier, blog.title))
    result.append("""<h2>Create new user</h2>
<form method="POST">
username: <input type="text" name="username"/><br/>
name: <input type="text" name="name"/><br/>
<input type="submit"/>
</form>""")
    return HttpResponse("".join(result))

def view_blog(request, blog):
    try:
        blog = models.Blog.objects.get(identifier=blog)
    except models.Blog.DoesNotExist:
        raise Http404#('No such blog: "%s"' % (blog,))
    if request.method == 'POST': return create_entry(request, blog)

    result = ['<h2>%s</h2>' % blog.title]
    for article in sorted(blog.articles.all(), key=(lambda entry:entry.date)):
        result.append('<a href="/blog/%s/%s">%s</a>' % (
                blog.identifier, article.id, article.title))
        result.append('<p>')
        result.append(article.text)
        result.append('</p>')
    result.append("""<h2>Create new article</h2>
<form method="POST">
title: <input type="text" name="title"/><br/>
username: <input type="text" name="username"/><br/>
text: <input type="text" name="text"/><br/>
<input type="submit"/>
</form>""")

    return HttpResponse("".join(result))

def view_article(request, article, blog):
    try:
        entry = models.Entry.objects.get(id=article)
        if entry.blog.identifier != blog: raise models.Entry.DoesNotExist
    except models.Entry.DoesNotExist:
        raise Http404#('No such article: "%s/%s"' % (blog,article))
    else:
        return HttpResponse(entry.text)

def view_user(request, user):
    try:
        user = models.User.objects.get(username=user)
    except models.User.DoesNotExist:
        raise Http404#('No such user: "%s"' % (user,))
    if request.method == 'POST': return create_blog(request, user)

    result = ['<h2>%s</h2>' % user.name]
    for blog in user.blogs.all():
        result.append('<a href="/blog/%s">%s</a><br/>' % (
                blog.identifier, blog.title))
    result.append("""<h2>Create new blog</h2>
<form method="POST">
id: <input type="text" name="identifier"/><br/>
title: <input type="text" name="title"/><br/>
<input type="submit"/>
</form>""")

    return HttpResponse("".join(result))

def create_user(request):
    print "User(username=%r,name=%r)" % (request.POST['username'],
                                         request.POST['name'])
    user = models.User.objects.create(username=request.POST['username'],
                                      name=request.POST['name'])
    return HttpResponseRedirect("/blog/~" + user.username)

def create_blog(request, user):
    blog = models.Blog(identifier=request.POST['identifier'],
                       title=request.POST['title'])
    blog.users.add(user)
    blog.save()
    return HttpResponseRedirect("/blog/" + blog.identifier)

def create_entry(request, blog):
    try:
        user = models.User.objects.get(username=request.POST['username'])
    except models.user.DoesNotExist:
        raise Http404#('No such user: "%s"' % (request.POST['username'],))
    for author in blog.users.all():
        if user == author: break
    else:
        return HttpResponse("FAILURE! the user is not a member")
    entry = models.Entry.objects.create(title=request.POST['title'],
                                        text=request.POST['text'],
                                        date=int(time.time()),
                                        blog=blog, author=user)
    path = request.path
    if not path.endswith('/'):
        path += '/'
    path += str(entry.id)
    return HttpResponseRedirect(request.path)
