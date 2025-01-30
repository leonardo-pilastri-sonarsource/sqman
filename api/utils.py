import os
import re

import requests

from api.properties import headers
from api.properties import installs_folder


def get_installed_sq(version):
    """Returns the list of installed SQ versions starting with version"""
    if not os.path.exists(installs_folder):
        return []
    return sorted([fold for fold in os.listdir(installs_folder) if fold.startswith(version)])


def get_online_versions(version):
    url = 'http://api.github.com/repos/SonarSource/sonarqube/tags?per_page=100'
    response = requests.get(url, headers=headers)
    pattern = re.compile(r'"name":\s*"(\d+\.\d+\.\d+\.?\d*)"')
    res = []
    for obj in pattern.findall(response.text):
        if version is None or obj.startswith(version):
            res.append(obj)
    return res
