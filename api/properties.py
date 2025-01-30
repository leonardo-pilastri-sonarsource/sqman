import os

home_folder = os.path.expanduser("~")
installs_folder = os.path.join(home_folder, '.sqman')
current_pid_file = os.path.join(installs_folder, 'sq_pid.txt')
