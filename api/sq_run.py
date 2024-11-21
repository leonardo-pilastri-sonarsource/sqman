import os.path
import platform
import subprocess

import click
import psutil as psutil
from click.testing import CliRunner

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
    shell = None
    if platform.system() == 'Windows':
        bin_path = os.path.join(install_path, 'bin', 'windows-x86-64', 'StartSonar.bat')
    elif platform.system() == 'Linux':
        shell = 'sh'
        bin_path = os.path.join(install_path, 'bin', 'linux-x86-64', 'sonar.sh')
    elif platform.system() == 'Darwin':
        shell = 'zsh'
        bin_path = os.path.join(install_path, 'bin', 'macosx-universal-64', 'sonar.sh')
    else:
        print('OS not supported')
        return

    try:
        process = [bin_path]
        if shell is not None:
            process.insert(0, shell)
            process.append('start')
        print(process)
        result = subprocess.Popen(process)
        print(result)
        # sys.stdout.write(result.stdout)
        # sys.stdout.flush()
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
