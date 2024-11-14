import os
import zipfile
from api.properties import installs_folder

import click
import requests

chunk_size = 8192
num_bars = 50

@click.command()
def installed():
    """Prints the list of installed SQ versions"""
    if not os.path.exists(installs_folder):
        print('No Sonarqube versions installed')
        return
    print('Installed Sonarqube versions:')
    for folder in os.listdir(installs_folder):
        print(folder)

@click.command()
@click.argument('sq_version', type=str)
def install(sq_version):
    """Downloads and installs a specific SQ version inside '~/.sqman' folder"""
    if os.path.exists(os.path.join(installs_folder, sq_version)):
        print('Sonarqube %s already installed' % sq_version)
        return

    url = 'https://binaries.sonarsource.com/Distribution/sonarqube/'

    # Versions <= 3.7 zips are not named 'sonarqube', but 'sonar' instead
    major_version = int(sq_version.split('.')[0])
    if major_version < 4:
        url += 'sonar-%s.zip' % sq_version
    else:
        url += 'sonarqube-%s.zip' % sq_version

    file_name = 'sonarqube%s.zip' % sq_version

    if not os.path.exists(installs_folder):
        os.makedirs(installs_folder, exist_ok=True)

    file_path = os.path.join(installs_folder, file_name)
    response = requests.get(url, stream=True)
    response.raise_for_status()  # Check for download errors
    if response.status_code != 200:
        print(f"Download error: {response}")
        return
    total_size = int(response.headers.get('content-length', 0))
    with open(file_path, "wb") as file:
        for chunk in response.iter_content(chunk_size=8192):  # Download in chunks
            file.write(chunk)
            downloaded_size = file.tell()
            progress = downloaded_size / total_size
            progress_bar = ('#' * int(progress * num_bars)).ljust(num_bars)
            print(f"\r[{progress_bar}] {progress:.2%}", end='')
    print(f"\nFile downloaded and saved to: {file_path}")
    unzip_and_rename(file_path, installs_folder, sq_version)


def unzip_and_rename(zip_path, extract_to, new_name):
    # Ensure the extraction folder exists
    os.makedirs(extract_to, exist_ok=True)

    # Unzip the file
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(extract_to)

    # Get the name of the extracted folder (assuming the zip contains a single root folder)
    extracted_folder = os.path.join(extract_to, zip_ref.namelist()[0].split('/')[0])

    # Define the new folder path
    new_folder_path = os.path.join(extract_to, new_name)

    # Rename the extracted folder
    if os.path.exists(extracted_folder):
        os.rename(extracted_folder, new_folder_path)
        print(f"Extracted folder renamed to: {new_folder_path}")
    else:
        print("Extraction failed or folder not found.")
    os.remove(zip_path)  # Remove the zip file
