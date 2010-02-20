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

from django import template

register = template.Library()

@register.tag(name="apply")
def do_apply(parser, token):
    contents = token.split_contents()
    tag_name = contents.pop(0)
    if not contents:
        raise template.TemplateSyntaxError(
            "%r tag requires at least a function argument" % tag_name)
    return ApplyNode(*contents)
   
class ApplyNode(template.Node):
    def __init__(self, func, *args):
        self._func = func
        self._args = args
    def render(self, context):
        func = context[self._func]
        args = [context[arg] for arg in self._args]
        return unicode(func(*args))
