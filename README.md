## SqMan
A python CLI application to manage, run, update your SonarQube local instances.

### Requirements

You need python installed on your system

### Setup

#### Linux/macOS Users:
Open a terminal and navigate to the directory containing the `install.sh` script \
Run the script using the command: `./install.sh`

#### Windows Users:
Open Command Prompt navigate to the directory containing the `install.bat` script \
Run the script using the command: `install.bat`

### How to use

After running the `install` script, simply type 
```
sqman
```
to get a list of available commands.

For each command you can type `sqman <command> --help` to see the usage of that specific command

### Contribute

If you want to change the code to improve this library, uncomment the `pip install --editable .` line in the `install`
script, this will make the CLI update on the fly with your changes.