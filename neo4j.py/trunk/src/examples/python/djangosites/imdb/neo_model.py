from neo4j.model import django_model as models

class Movie(models.NodeModel):
    title = models.Property(indexed=True)
    year = models.Property()
    href = property(lambda self: ('/movie/%s/' % (self.node.id,)))
    def __unicode__(self):
        return self.title

class Actor(models.NodeModel):
    name = models.Property(indexed=True)
    def __unicode__(self):
        return self.name

class Role(models.NodeModel):
    name = models.Property(indexed=True)
    actors = models.Relationship(Actor, type=models.Incoming.portrays,
                                 related_name="roles")
    movie = models.Relationship(Movie, type=models.Outgoing.in_movie,
                                single=True, optional=False,
                                related_name="parts")
    def __unicode__(self):
        return u'"%s" in "%s"' % (self.name, self.movie.title)

