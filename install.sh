#!/bin/bash

# Create a virtual environment only if it does not exist
if [ ! -d ".venv" ]; then
    python3 -m venv .venv/
fi

# Activate the virtual environment
source .venv/bin/activate
pip install -r requirements.txt

# Use the editable option if you want to change the scripts and try the on the fly
pip install --editable .