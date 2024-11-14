import os.path
import sys

import click
from click.testing import CliRunner

from api import sq_install
from api.properties import installs_folder

runner = CliRunner()


@click.command()
@click.argument('sq_version', type=str)
def start(sq_version):
    """Starts an installed SQ instance"""
    install_path = os.path.join(installs_folder, sq_version)
    if not os.path.exists(install_path):
        print('Sonarqube %s is not installed' % sq_version)
        answer = click.prompt('Do you want to install it now? Y/N', default='N', type=str)
        if answer == 'N':
            return
        else:
            print('Installing Sonarqube %s ' % sq_version)
            result = runner.invoke(sq_install.install, [sq_version])
            sys.stdout.write(result.stdout)
            sys.stdout.flush()
    print('Starting Sonarqube %s ...' % sq_version)
