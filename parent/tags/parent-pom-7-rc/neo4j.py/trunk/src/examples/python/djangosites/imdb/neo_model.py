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

from neo4j.model import django_model as models

class Movie(models.NodeModel):
    title = models.Property(indexed=True)
    year = models.Property()
    href = property(lambda self: ('/movie/%s/' % (self.node.id,)))
    def __unicode__(self):
        return self.title

class Actor(models.NodeModel):
    name = models.Property(indexed=True)
    href = property(lambda self: ('/actor/%s/' % (self.node.id,)))
    def __unicode__(self):
        return self.name

class Role(models.NodeModel):
    name = models.Property(indexed=True)
    actors = models.Relationship(Actor, type=models.Incoming.portrays,
                                 related_name="roles")
    movie = models.Relationship(Movie, type=models.Outgoing.in_movie,
                                single=True, optional=False,
                                related_name="parts")
    href = property(lambda self: ('/role/%s/' % (self.node.id,)))
    @property
    def actor(self):
        for actor in self.actors.all():
            return actor
    def __unicode__(self):
        return u'"%s" in "%s"' % (self.name, self.movie.title)

