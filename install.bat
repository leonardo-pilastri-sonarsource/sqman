@echo off
:: Requires python installed on the system

python -m venv .venv/

:: Activate the virtual environment
call .venv\Scripts\activate
pip install -r requirements.txt
pip install --editable .