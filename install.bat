@echo off

:: Activate the virtual environment
call .wvenv\Scripts\activate
pip install -r requirements.txt
pip install --editable .