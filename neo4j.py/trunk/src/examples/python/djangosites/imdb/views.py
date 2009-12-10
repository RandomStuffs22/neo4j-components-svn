from django.shortcuts import render_to_response
from django.http import HttpResponse, HttpResponseRedirect, Http404
from django.core.urlresolvers import reverse

from djangosites.imdb import models

def index(request):
    return render_to_response('imdb/index.html', {})

search_redirects = {
    'all': 'djangosites.imdb.views.index',
}

class search_functions:
    def __metaclass__(name, bases, body):
        queries = [key for key in body if not key.startswith('_')]
        obj = type(name, bases, body)(queries)
        return dict((key, getattr(obj, key)) for key in queries)
    def __init__(self, queries):
        self.__queries = [getattr(self, query) for query in queries
                          if query != 'all']

    def all(self, query):
        for search in self.__querires:
            for item in search( query ):
                yield item

    def title(self, query):
        return models.Movie.objects.filter(title=query)
    def names(self, query):
        return models.Actor.objects.filter(name=query)
    def keyword(self, query):
        for Model in (models.Movie, models.Actor):
            for element in Model.objects.filter(keywords=query):
                yield element
    def character(self, query):
        return models.Roles.objects.filter(name=query)
    

def search(request):
    search = request.POST.get('search', 'all')
    query = request.POST.get('query', '')
    if query:
        search = search_functions.get(search, search_functions['all'])
        return render_to_response('imdb/list.html', {
                'title': "Search results",
                'items': search( query ),
            })
    else:
        return HttpResponseRedirect(reverse(search_redirects.get(
                    search, search_redirects['all'])))

def list_movies(request):
    return render_to_response('imdb/list.html', {
            'title': "List of Movies",
            'items': models.Movie.objects.all(),
        })


#def actor(Actor, Movie, request, backend, id=None):
#    """Actor page - list movies"""
#    if id is None: # Search or search form
#        if request.method == 'POST': # Search
#            try:
#                actor = Actor.objects.get(name=request.POST['name'])
#            except Actor.DoesNotExist:
#                return render_to_response('error.html', dict(
#                        backend=backend,
#                        message=('Could not find the actor "%s".' %
#                                 request.POST['name'])))
#        else: # Search form
#            return render_to_response(
#                'search.html',
#                dict(title="Lookup Actor",
#                     qname="name",
#                     legend="Actor name:",
#                     submit="Lookup",
#                     backend=backend,
#                     ))
#    else:
#        try:
#            actor = Actor.objects.get(id=long(id))
#        except Actor.DoesNotExist:
#            return render_to_response('error.html', dict(
#                    backend=backend,
#                    message="There is no such actor."))
#    try:
#        return render_to_response(
#            'actor.html', # Name of template file
#            dict(title=actor.name, # parameters needed by the template
#                 backend=backend,
#                 actor=actor, # this is the actual actor object
#                 # properties on the object may be accessed from the template
#                 ))
#    finally:
#        actor.save()
#
#def movie(Actor, Movie, request, backend, id=None):
#    """Movie page - list actors"""
#    if id is None: # Search or serach form
#        if request.method == 'POST': # Search
#            try:
#                movie = Movie.objects.get(title=request.POST['title'])
#            except Movie.DoesNotExist:
#                return render_to_response('error.html', dict(
#                        backend=backend,
#                        message=('Could not find the movie "%s".' %
#                                 request.POST['title'])))
#        else: # Search form
#            return render_to_response(
#                'search.html',
#                dict(title="Lookup Movie",
#                     qname="title",
#                     legend="Movie title:",
#                     submit="Lookup",
#                     backend=backend,
#                     ))
#    else:
#        try:
#            movie = Movie.objects.get(id=long(id))
#        except Movie.DoesNotExist:
#            return render_to_response('error.html', dict(
#                    backend=backend,
#                    message="There is no such movie."))
#    try:
#        return render_to_response(
#            'movie.html',
#            dict(title=movie.title,
#                 backend=backend,
#                 movie=movie,
#                 ))
#    finally:
#        movie.save()
