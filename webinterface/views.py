from django.shortcuts import render
from django.views.decorators.csrf import ensure_csrf_cookie


@ensure_csrf_cookie
def goal_glance(request):
    return render(request, 'webinterface/goal/glance.html')
