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

from django.db import models

class Actor(models.Model):
    name = models.TextField()
    href = property(lambda self: ('/movie/%s/' % (self.id,)))
    def __unicode__(self):
        return self.name

class Movie(models.Model):
    title = models.TextField()
    year = models.IntegerField()
    href = property(lambda self: ('/actor/%s/' % (self.id,)))
    def __unicode__(self):
        return self.title

class Role(models.Model):
    name = models.TextField()
    actors = models.ManyToManyField(Actor, related_name="roles")
    movie = models.ForeignKey(Movie, related_name="parts")
    href = property(lambda self: ('/role/%s/' % (self.id,)))
    @property
    def actor(self):
        for actor in self.actors.all():
            return actor
    def __unicode__(self):
        return u"%s as %s in %s"%(self.actor.name, self.name, self.movie.title)

