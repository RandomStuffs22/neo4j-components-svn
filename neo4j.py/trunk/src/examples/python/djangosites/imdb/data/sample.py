from __future__ import with_statement
from djangosites.imdb import models, importer
monroe = models.Role.objects.get(name='Reverend Monroe')
assert monroe is not None, "Reverend Monroe should exist"
cold_mountain = monroe.movie
assert cold_mountain is not None, "a Role must be related to a Movie"
assert cold_mountain.title == 'Cold Mountain (2003)'
print "\nSucessfully loaded", monroe
