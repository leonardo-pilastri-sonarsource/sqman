import os

import click

from api.properties import installs_folder


def get_installed_sq(version):
    """Returns the list of installed SQ versions starting with version"""
    if not os.path.exists(installs_folder):
        return []
    return sorted([fold for fold in os.listdir(installs_folder) if fold.startswith(version)])

