## SqMan
A python CLI application to manage, run, update your SonarQube local instances.

### Requirements

You need python installed on your system

### Setup

#### Linux/macOS Users:
Open a terminal and navigate to the directory containing the `install.sh` script \
Run the script using the command: `source install.sh`

#### Windows Users:
Open Command Prompt navigate to the directory containing the `install.bat` script \
Run the script using the command: `install.bat`

This will also activate the python virtual environment.

### How to use

After running the `install` script, simply type 
```
sqman
```
to get a list of available commands.

For each command you can type `sqman <command> --help` to see the usage of that specific command



## TODO List

* The `install-plugin` command gets 401 on repox because it is not authenticated, is there a way to download public builds without auth?
* Add local file to store the last sq version installed/used to automatically use when running `sqman run` without a 
version argument