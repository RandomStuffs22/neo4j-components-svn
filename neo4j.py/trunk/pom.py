# -*- coding: utf-8 -*-

import xml.etree.ElementTree as ElementTree
import re, os.path, posixpath, sys
try: # Python 2.x
    from urllib2 import urlopen
except: # Python 3.x
    from urllib.request import urlopen
try: # More Python 3.x fallback
    isinstance('', basestring)
except:
    basestring = str
try:
    set()
except:
    from sets import Set as set

__all__ = ('Pom',)

class Pom(object):
    pattern = re.compile(r"\${(?P<var>[^}]*)}")
    def __init__(self, tree, repositories=(), parent=True):
        if isinstance(tree, basestring):
            self.path = tree
            self.tree = tree = ElementTree.parse(tree)
        else:
            self.path = None
            self.tree = tree
        if hasattr(tree, 'tag'):
            tag = tree.tag
        else:
            tag = tree.getroot().tag
        ns = re.match('^{([^}]*)}', tag)
        if ns:
            self.__ns = ns.groups()[0]
        else:
            self.__ns = None
        self.repositories = repositories
        self.parent = None
        if parent:
            self.parent = ParentPom(self)
    def __path(self, path):
        if self.__ns:
            nspath = '{%s}%s' % (
                self.__ns, path.replace('.', '/{%s}' % (self.__ns,)))
        else:
            nspath = path.replace('.', '/')
        return nspath
    def __getitem__(self, path):
        element = self.tree.findtext(self.__path(path))
        if element is None:
            element = self.tree.findtext(self.__path('properties.' + path))
        if element is None and self.parent is not None:
            element = self.parent[path]
        if element is None:
            raise KeyError(path)
        return self.pattern.sub(lambda match:self[match.groupdict()['var']],
                                element)
    def developer(self, developer_role, element):
        for developer in self.tree.findall(self.__path('developers.developer')):
            for role in developer.findall(self.__path('roles.role')):
                if role.text.strip() == developer_role:
                    return developer.findtext(self.__path(element)).strip()
    def download_dependencies(self, destination):
        result = set()
        if not os.path.exists(destination):
            os.mkdir(destination)
        for dep in self.tree.findall(self.__path('dependencies.dependency')):
            try:
                group = dep.find(self.__path('groupId')).text
                artifact = dep.find(self.__path('artifactId')).text
                version = dep.find(self.__path('version')).text
            except:
                raise ValueError("Pom structure error.")
            else:
                scope = dep.findtext('scope', 'runtime').strip().lower()
                if scope == 'test': continue
                pom, jar = download_dependency(destination, self.repositories,
                                               group, artifact, version)
                result.add((jar, scope == 'runtime'))
                for jar, include in pom.download_dependencies(destination):
                    result.add((jar, include))
        for item in result: yield item

def ParentPom(pom):
    class ParentPom(object):
        def __init__(self, id, pom):
            self.id = id
            self.__pom = pom
        def __getitem__(self, path):
            return replacement.get(path, lambda s,x:x)(self, self.__pom[path])
    replacement = {
        'url': lambda pom, value: posixpath.join(value, pom.id)
        }
    try:
        group = pom['parent.groupId']
        artifact = pom['parent.artifactId']
        version = pom['parent.version']
        try:
            path = pom['parent.relativePath']
            tree = os.path.join(os.path.dirname(pom.path), path)
            assert os.path.exists(tree)
        except:
            data = get_data(pom.repositories, group, artifact, version, version,
                            '%(artifact)s-%(version)s.pom')
            tree = ElementTree.fromstring(data)
    except:
        return None
    else:
        return ParentPom(
            pom['artifactId'],
            Pom(tree, repositories=pom.repositories, parent=False))

def download_dependency(destination, repositories, group, artifact, version):
    pom_file = os.path.join(destination, artifact + '.pom')
    jar_file = os.path.join(destination, artifact + '.jar')
    if os.path.exists(pom_file) and os.path.exists(jar_file):
        pom = Pom(pom_file, repositories=repositories)
        if pom['groupId'] == group\
                and pom['artifactId'] == artifact\
                and pom['version'] == version:
            return pom, jar_file
    if version.endswith('SNAPSHOT'):
        realver = get_latest_version(repositories, group, artifact, version)
    else:
        realver = version
    download(pom_file, repositories, group, artifact, version, realver,
             '%(artifact)s-%(version)s.pom')
    download(jar_file, repositories, group, artifact, version, realver,
             '%(artifact)s-%(version)s.jar')
    return Pom(pom_file, repositories=repositories), jar_file

def get_latest_version(repositories, group, artifact, version):
    metadata = Pom(ElementTree.fromstring(get_data(
                repositories, group, artifact, version, 'maven-metadata.xml')),
                   repositories=repositories, parent=False)
    timestamp = metadata['versioning.snapshot.timestamp']
    buildNumber = metadata['versioning.snapshot.buildNumber']
    return version.replace("SNAPSHOT", "%s-%s" % (timestamp, buildNumber))

def download(target_file, repositories, group, artifact,
             version, real_version, pattern):
    data = get_data(repositories,group,artifact,version,real_version,pattern)
    file = open(target_file, 'wb')
    try:
        file.write(data)
    finally:
        file.close()

def get_data(repositories, group, artifact, version, real_version, pattern):
    for repo in repositories:
        url = posixpath.join(repo, '/'.join(group.split('.') + [
                    artifact, version, pattern % {
                        'artifact': artifact, 'version': real_version,
                        }]))
        try:
            source = urlopen(url)
            try:
                return source.read()
            finally:
                source.close()
        except:
            continue
    raise ValueError("Could not find artifact %s/%s-%s" % (
            group, artifact, version))

