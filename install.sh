#!/bin/bash

# Activate the virtual environment
source .uvenv/bin/activate
pip install -r requirements.txt
pip install --editable .