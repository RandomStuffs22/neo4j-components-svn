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
