import re

import click
import requests

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
}


@click.command()
@click.option('-l', '--limit', help='Limits the amount of results', type=int, default=10, show_default=True)
@click.option('-v', '--version', help='Search for versions starting with this value', type=str, required=False)
def list(limit, version):
    """Prints the list of available SQ versions online"""
    url = 'http://api.github.com/repos/SonarSource/sonarqube/tags?per_page=100'
    response = requests.get(url, headers=headers)
    pattern = re.compile(r'"name":\s*"(\d+\.\d+\.\d+\.?\d*)"')
    output = ""
    count = 0
    for obj in pattern.findall(response.text):
        if count < limit and (version is None or obj.startswith(version)):
            output += 'SonarQube ' + obj + '\n'
            count += 1
    click.echo(output)


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
