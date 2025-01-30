import os

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
}

home_folder = os.path.expanduser("~")
installs_folder = os.path.join(home_folder, '.sqman')
current_pid_file = os.path.join(installs_folder, 'sq_pid.txt')
