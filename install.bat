@echo off
:: Requires python installed on the system
if not exist .venv\ (
    python -m venv .venv/
)
:: Activate the virtual environment
call .venv\Scripts\activate
pip install -r requirements.txt

:: Use the editable option if you want to change the scripts and try the on the fly
::pip install --editable .
pip install .