import os.path
import platform
import subprocess
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
    if platform.system() == 'Windows':
        bin_path = os.path.join(install_path, 'bin/windows-x86-64/StartSonar.bat')
    elif platform.system() == 'Linux':
        bin_path = os.path.join(install_path, 'bin/linux-x86-64/sonar.sh')
    elif platform.system() == 'Darwin':
        bin_path = os.path.join(install_path, 'bin/macosx-universal-64/sonar.sh')
    else:
        print('OS not supported')
        return

    try:
        result = subprocess.run([bin_path], stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        sys.stdout.write(result.stdout)
        sys.stdout.flush()
    except subprocess.CalledProcessError as e:
        print("An error occurred:", e)
        print("Error output:", e.stderr)

