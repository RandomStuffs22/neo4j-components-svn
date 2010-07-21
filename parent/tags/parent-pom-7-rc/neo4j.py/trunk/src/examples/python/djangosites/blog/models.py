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

class Blog(models.NodeModel):
    identifier = models.Property(indexed=True)
    title = models.Property()
    def __unicode__(self):
        return self.title

class User(models.NodeModel):
    username = models.Property(indexed=True)
    name = models.Property()
    blogs = models.Relationship(Blog, type=models.Outgoing.member_of,
                                related_name="users")
    def __unicode__(self):
        return self.name

class Entry(models.NodeModel):
    title = models.Property()
    text = models.Property()
    date = models.Property()
    blog = models.Relationship(Blog, type=models.Outgoing.posted_on,
                               single=True, optional=False,
                               related_name="articles")
    author = models.Relationship(User, type=models.Outgoing.authored_by,
                                 single=True, optional=False,
                                 related_name="articles")
    def __unicode__(self):
        return self.title

class Comment(models.NodeModel):
    text = models.Property()
    article = models.Relationship(Entry, type=models.Outgoing.comment_to,
                                  single=True, optional=False,
                                  related_name="comments")
    author = models.Relationship(User, type=models.Outgoing.written_by,
                                 single=True, optional=False,
                                 related_name="comments")
    def __unicode__(self):
        return u'comment to "%s" by "%s"' % (self.article, self.author)
