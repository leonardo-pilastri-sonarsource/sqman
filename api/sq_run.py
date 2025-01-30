import os.path
import platform
import subprocess
import time

import click
import psutil as psutil
from click.testing import CliRunner

from api.properties import current_pid_file
from api.properties import installs_folder
from api.utils import get_installed_sq

runner = CliRunner()


@click.command()
@click.argument('sq_version', type=str, default="")
def run(sq_version):
    """Starts an installed SQ instance"""
    choices = get_installed_sq(sq_version)
    if len(choices) == 0:
        print('Sonarqube %s is not installed' % sq_version)
        print('Run "sqman install %s" to install it' % sq_version)
        return
    if len(choices) > 1:
        print('Must specify one between:')
        for v in choices: print(v)
        return
    torun = choices[0]
    install_path = os.path.join(installs_folder, torun)
    if not os.path.exists(install_path):
        print('Sonarqube %s is not installed' % torun)
        print('Run "sqman install %s" to install it' % torun)
        return
    print('Starting Sonarqube %s ...' % torun)
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
        print(f"Running command: {' '.join(process)}")
        subprocess.Popen(process, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

        # Wait a few seconds for SonarQube to initialize
        time.sleep(3)

        # Find the actual Java process running SonarQube
        real_pid = find_sonarqube_pid()
        if real_pid:
            print(f"SonarQube started with PID: {real_pid}")
            with open(current_pid_file, "w") as pid_file:
                pid_file.write(str(real_pid))
        else:
            print("Failed to detect SonarQube PID. It may not have started correctly.")

        with open(current_pid_file, "w") as pid_file:
            pid_file.write(str(real_pid))
    except subprocess.CalledProcessError as e:
        print("An error occurred:", e)
        print("Error output:", e.stderr)


@click.command()
def stop():
    """Stops the current running SQ instance"""
    if not os.path.exists(current_pid_file):
        print(f"No PID file found. SonarQube may not be running.")
        return
    try:
        # Read the PID from the file
        with open(current_pid_file, "r") as pid_file:
            pid = int(pid_file.read().strip())

        # Use psutil to stop the process
        process = psutil.Process(pid)
        process.terminate()  # Gracefully stop
        try:
            process.wait(timeout=5)  # Wait for shutdown
        except psutil.TimeoutExpired:
            print("Process did not terminate in time, forcing kill...")
            process.kill()  # Force kill if needed

        print(f"SonarQube instance with PID {pid} has been stopped.")
        os.remove(current_pid_file)
    except psutil.NoSuchProcess:
        print(f"No process with PID {pid} found.")
    except FileNotFoundError:
        print("PID file not found.")
    except Exception as e:
        print(f"Error stopping the process: {e}")


@click.command()
def status():
    """Prints the status of the running SonarQube instance"""
    if not os.path.exists(current_pid_file):
        print("SonarQube is not running (PID file not found).")
        return

    try:
        with open(current_pid_file, "r") as pid_file:
            pid = int(pid_file.read().strip())

        if psutil.pid_exists(pid):
            process = psutil.Process(pid)
            print(f"SonarQube is running (PID: {pid}, Status: {process.status()}).")
        else:
            print(f"SonarQube is not running (No process with PID {pid}).")
            os.remove(current_pid_file)  # Cleanup stale PID file

    except (FileNotFoundError, ValueError):
        print("Invalid or missing PID file. SonarQube might not be running.")
    except psutil.NoSuchProcess:
        print("SonarQube is not running (Process does not exist).")
        os.remove(current_pid_file)  # Cleanup stale PID file
    except Exception as e:
        print(f"Error checking SonarQube status: {e}")


def find_sonarqube_pid():
    """Finds the PID of the SonarQube Java process."""
    for process in psutil.process_iter(attrs=["pid", "name", "cmdline"]):
        try:
            if "java" in process.info["name"].lower():
                cmdline = " ".join(process.info["cmdline"])
                if ".sqman" in cmdline.lower():  # Ensure it's SonarQube from sqman folder
                    return process.info["pid"]
        except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
            continue
    return None
