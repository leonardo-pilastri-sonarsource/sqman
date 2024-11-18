#!/bin/bash

# Create a virtual environment only if it does not exist
if [ ! -d ".uvenv" ]; then
    python3 -m venv .uvenv/
fi

# Activate the virtual environment
source .uvenv/bin/activate
pip install -r requirements.txt

# Use the editable option if you want to change the scripts and try the on the fly
pip install --editable .