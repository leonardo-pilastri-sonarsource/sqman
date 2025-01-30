import re

import click
import requests

from api import utils
from api.properties import headers


@click.command()
@click.argument('sq_version', type=str, default="")
@click.option('-l', '--limit', help='Limits the amount of results', type=int, default=20, show_default=True)
def list(sq_version, limit):
    """Prints the list of available SQ versions online starting with sq_version"""
    versions = utils.get_online_versions(sq_version)
    count = 0
    for obj in versions:
        if count < limit:
            print('SonarQube ' + obj)
            count += 1


@click.command()
@click.argument('sq_version', type=str)
def plugins(sq_version):
    """Prints the embedded plugins of a specific SQ version"""
    major_version = int(sq_version.split('.')[0])
    if major_version >= 8:
        url = 'http://raw.githubusercontent.com/SonarSource/sonarqube/%s/build.gradle' % sq_version
        pattern = re.compile(r"dependency\s+'([^']+-plugin[^']*)'")
    elif major_version >= 7:
        # TODO Still not catching versions with ${slangVersion}, and the final @jar should be removed
        url = 'http://raw.githubusercontent.com/SonarSource/sonarqube/%s/sonar-application/build.gradle' % sq_version
        pattern = re.compile(r"bundledPlugin\s+'([^']+-plugin[^']*)'")
    else:
        print("Version not supported")
        return
    print(url)
    output = ""
    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        print("ERROR", response)
        return
    for line in response.text.split('\n'):
        match = pattern.search(line)
        if match:
            group = match.group(1).split(':')[1:]
            output += " ".join(group) + '\n'
    click.echo(output)
