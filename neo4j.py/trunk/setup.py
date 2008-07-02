from distutils.core import setup

setup(
    name="Neo4j.py",
    version='0.1',
    description="Python bindings for Neo4J",
    author="Tobias Ivarsson",
    author_email="tobias.ivarsson@neotechnology.com",
    url="http://neo4j.org/community/neo4jpy/",
    packages=['neo4j'],
    package_dir={'neo4j':'src/neo4j'},
    license="""Copyright 2008 Network Engine for Objects in Lund AB [neotechnology.com]

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
""",
    )
