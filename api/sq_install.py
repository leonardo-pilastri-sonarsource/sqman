import os
import zipfile

import click
import requests

home_folder = os.path.expanduser("~")
installs_folder = os.path.join(home_folder, '.sqman/')


@click.command()
@click.argument('sq_version', type=str)
def install(sq_version):
    """Downloads and installs a specific SQ version inside '~/.sqman' folder"""
    # TODO: wrong link, this downloads the code, not the binaries
    url = 'http://api.github.com/repos/SonarSource/sonarqube/zipball/refs/tags/%s' % sq_version
    file_name = 'sonarqube%s.zip' % sq_version

    if not os.path.exists(installs_folder):
        os.makedirs(installs_folder, exist_ok=True)

    file_path = os.path.join(installs_folder, file_name)
    response = requests.get(url, stream=True)
    response.raise_for_status()  # Check for download errors
    with open(file_path, "wb") as file:
        for chunk in response.iter_content(chunk_size=8192):  # Download in chunks
            file.write(chunk)
    print(f"File downloaded and saved to: {file_path}")
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