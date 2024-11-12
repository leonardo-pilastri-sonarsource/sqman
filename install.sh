#!/bin/bash

python3 -m venv .uvenv/

# Activate the virtual environment
source .uvenv/bin/activate
pip install -r requirements.txt
pip install --editable .