import os.path
import platform
import subprocess
import sys

import click
import psutil as psutil
from click.testing import CliRunner

from api import sq_install
from api.properties import installs_folder

runner = CliRunner()

@click.command()
@click.argument('sq_version', type=str)
def run(sq_version):
    """Starts an installed SQ instance"""
    install_path = os.path.join(installs_folder, sq_version)
    if not os.path.exists(install_path):
        print('Sonarqube %s is not installed' % sq_version)
        print('Run "sqman install %s" to install it' % sq_version)
        return
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
        sq_process = subprocess.run([bin_path], stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
        os.environ['SQ_PID'] = str(sq_process.pid)
        print(f"Started subprocess with PID: {sq_process.pid}")
    except subprocess.CalledProcessError as e:
        print("An error occurred:", e)
        print("Error output:", e.stderr)


@click.command()
def status():
    """Checks the status of the running SQ instance"""
    if 'SQ_PID' not in os.environ:
        print('SQ instance not started yet.')
        return
    pid = int(os.environ['SQ_PID'])
    sq_process = psutil.Process(pid)
    print(sq_process.status())
