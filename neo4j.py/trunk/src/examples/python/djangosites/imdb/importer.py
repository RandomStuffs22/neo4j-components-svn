#!/bin/sh
# -*- mode: Python; coding: utf-8 -*-

# Copyright (c) 2008-2009 "Neo Technology,"
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

"""echo\
" IMDB data importer wrapper script

# This is a Python module first and foremost,
# but this first string makes it possible to execute the file as a shell script

# Set up the datadir parameter before we change dir to get relative paths right
if [ -n "$IMDB_DATADIR" ]; then
    IMDB_DATADIR=`cd $IMDB_DATADIR; pwd`
fi

WORKDIR=`pwd`

# locate this file
EXE=$0
while [ -L "$EXE" ]; do
    EXE=`readlink $EXE`
done
# cd into the parent directory (this is where we'll find manage.py)
cd `dirname $EXE`/..
DIR=`pwd`

# verify that we have found manage.py
if [ ! -f "$DIR/manage.py" ]; then
    echo ERROR! Could not find manage.py
    exit 1
fi

# Locate the appropriate python/jython interpreter
if [ -n "$VIRTUAL_ENV" ]; then
    for EXECUTABLE in `ls $VIRTUAL_ENV/bin`; do
        if [ "$EXECUTABLE" == "jython" ]; then
            PYTHON=$VIRTUAL_ENV/bin/jython
        elif [ "$EXECUTABLE" == "python" ]; then
            PYTHON=$VIRTUAL_ENV/bin/python
        fi
    done
fi
if [ -z "$PYTHON" ]; then
    echo WARNING! Using default Python
    PYTHON=python
fi

# Find out the name of the base Django module
PACKAGE=`basename $DIR`

COMMAND="from $PACKAGE.imdb import importer\n"

while [ $# -gt 0 ]; do
    case "$1" in
        sql)
            export DJANGOSITES_MODEL_TYPE=sql
            ;;
        orm)
            export DJANGOSITES_MODEL_TYPE=sql
            ;;
        neo)
            export DJANGOSITES_MODEL_TYPE=neo4j
            ;;
        exec)
            SCRIPT=$2
            if [ -z "$SCRIPT" ]; then
                echo "Usage: $0 $1 <scriptfile.py>"
                exit 1
            fi
            if [ ! -f "$SCRIPT" ]; then
                SCRIPT=$WORKDIR/$SCRIPT
                if [ ! -f "$SCRIPT" ]; then
                    echo $2 does not exist
                    exit 1
                fi
            fi
            $PYTHON manage.py shell < $SCRIPT
            exit
            ;;
        shell)
            if [ -n "`which rlwrap`" ]; then
                PYTHON="rlwrap $PYTHON"
            fi
            $PYTHON manage.py shell
            exit
            ;;
        *)
            COMMAND=$COMMAND"importer.$1()\n"
            ;;
    esac
    shift
done

# Create the command that imports and executes this module
COMMAND=$COMMAND"\
importer.populate(\"$IMDB_DATADIR\")\n\
"

# Execute in the Django shell
echo $COMMAND | $PYTHON manage.py shell

# End of the shell script part of this file - the rest is Python
exit "\
$?"""
# Beging Python code
from __future__ import division, with_statement, absolute_import

# Override the doc string, the shell script is not useful as documentation.
__doc__ = """
Import IMDB data.

 Module for importing data into the IMDB data model.

 The main entry point for this module is the populate function.


 Copyright (c) 2009 "Neo Technology,"
     Network Engine for Objects in Lund AB [http://neotechnology.com]
"""

import sys, os, contextlib, time

from . import models # completely portable
from neo4j.model.django_model import NeoServiceProperty


def print_percent(cur, tot):
    per = int((cur / tot) * 100)
    sys.stdout.write("\x08\x08\x08\x08%3d%%" % per)
    sys.stdout.flush()

def importer(file, pattern, factory, batch):
    import re
    pattern = re.compile(pattern)
    with open(file) as data:
        data.seek(0,2); size = data.tell(); data.seek(0)
        line = data.readline()
        last = None
        while line:
            print_percent(data.tell(), size)
            match = pattern.match(line)
            if match:
                last = factory(batch, last, **match.groupdict())
            line = data.readline()
        print("\x08\x08\x08\x08done.")

def make_movie(batch, last, title, year):
    batch.add( models.Movie(title=title, year=int(year)) )

def make_actor(batch, last, actor, movie, role):
    if actor is not None:
        last = models.Actor.objects.create(name=actor)
        #batch.add( last )
    if role is None:
        role = "self"
    the_movie = models.Movie.objects.get(title=movie)
    role = models.Role.objects.create(movie=the_movie, name=role)
    role.actors.add( last )
    batch.add( role )
    return last

def import_movies(file, batch):
    pattern = r"^(?P<title>[^\t]+)\t+(?P<year>[0-9]+)$"
    importer(file, pattern, make_movie, batch)

def import_actors(file, batch):
    pat = r"(?P<actor>[^\t]+)?\t+(?P<movie>[^\t\n]+)(\t+\[(?P<role>[^\]]+)\])?$"
    importer(file, pat, make_actor, batch)


class BatchProcessor(object):
    def __init__(self, batch_size=100):
        self.__size = batch_size
        self.__objects = []
    def add(self, obj):
        self.__objects.append(obj)
        if len(self.__objects) > self.__size:
            self.save()
    def save(self):
        objects, self.__objects = self.__objects, []
        with self.transaction:
            for obj in objects:
                obj.save()
    @property
    @contextlib.contextmanager
    def transaction(self):
        from django.conf import settings
        if settings.MODEL_TYPE == 'neo4j':
            with self.__neo.transaction:
                yield
        else:
            yield
    __neo = NeoServiceProperty()


def populate(datadir=None, batch_size=100):
    batcher = BatchProcessor(batch_size)
    if not datadir:
        datadir=os.path.join(os.path.abspath(os.path.dirname(__file__)),'data')
    print "Importing movies..."
    import_movies(os.path.join(datadir, 'test-movies.list'), batcher)
    batcher.save() # save any residual elements
    print "Importing actors and roles..."
    import_actors(os.path.join(datadir, 'test-actors.list'), batcher)
    batcher.save() # save any residual elements
    print "done."

def deltree(dirname):
    if os.path.exists(dirname):
        for root,dirs,files in os.walk(dirname):
            for dir in dirs:
                deltree(os.path.join(root,dir))
            for file in files:
                os.remove(os.path.join(root,file))
        os.rmdir(dirname)

def drop():
    print "dropping database tables"
    raise NotImplementedError

def clear():
    from django.conf import settings
    from django.db import connection
    print("clearing databases")
    deltree(settings.NEO4J_RESOURCE_URI)
    print("neo4j store deleted")
    cursor = connection.cursor()
    cursor.execute("DELETE FROM imdb_role_actors;")
    cursor.execute("DELETE FROM imdb_role;")
    cursor.execute("DELETE FROM imdb_actor;")
    cursor.execute("DELETE FROM imdb_movie;")
    cursor.commit()
    print("relational database cleared")

def done():
    sys.exit()
