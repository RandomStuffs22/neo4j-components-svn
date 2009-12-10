from django.shortcuts import render_to_response
from django.http import HttpResponse, HttpResponseRedirect, Http404
from django.core.urlresolvers import reverse
from django.template.loader import render_to_string

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
        for search in self.__queries:
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
        return models.Role.objects.filter(name=query)
    

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

def _list(title, Model):
    return render_to_response('imdb/list.html', {
            'title': title,
            'items': Model.objects.all(),
        })

def list_movies(request):
    return _list("List of Movies", models.Movie)

def list_actors(request):
    return _list("List of Actors", models.Actor)

def _view(Model, entity_id, *attributes):
    entity = Model.objects.get(id=int(entity_id))
    return render_to_response('imdb/view.html', {
            'title': str(entity),
            'attributes': attributes,
            'item': entity,
        })

def view_movie(request, movie):
    return _view(models.Movie, movie, _movie_parts)

def view_actor(request, actor):
    return _view(models.Actor, actor, _actor_roles)

def view_role(request, role):
    return _view(models.Role, role, _role_movie, _role_actors)

def _movie_parts(movie):
    return render_to_string('imdb/parts_table.html', {
            'parts': movie.parts.all(),
        })

def _actor_roles(actor):
    return render_to_string('imdb/list_view.html', {
            'title': "Roles",
            'items': actor.roles.all(),
        })

def _role_movie(role):
    return '<a href="/imdb%s">%s</a>' % (role.movie.href, role.movie)

def _role_actors(role):
    return render_to_string('imdb/list_view.html', {
            'title': "Played by",
            'items': role.actors.all(),
        })
