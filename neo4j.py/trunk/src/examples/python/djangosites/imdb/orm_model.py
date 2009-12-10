from django.db import models

class Actor(models.Model):
    name = models.TextField()
    def __unicode__(self):
        return self.name

class Movie(models.Model):
    title = models.TextField()
    year = models.IntegerField()
    def __unicode__(self):
        return self.title

class Role(models.Model):
    name = models.TextField()
    actors = models.ManyToManyField(Actor, related_name="roles")
    movie = models.ForeignKey(Movie, related_name="parts")
    @property
    def actor(self):
        for actor in self.actors.all():
            return actor
    def __unicode__(self):
        return u"%s as %s in %s"%(self.actor.name, self.name, self.movie.title)

