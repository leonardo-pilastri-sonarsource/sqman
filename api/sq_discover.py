import json
import re

import click
import requests

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
}


@click.command()
@click.option('-l', '--limit', help='Limits the amount of results', type=int, default=10)
@click.option('-v', '--version', help='Search for the specified version', type=str, required=False)
def list(limit, version):
    """Prints the list of available SQ versions online"""
    response = requests.get('http://api.github.com/repos/SonarSource/sonarqube/tags?per_page=100', headers=headers)
    pattern = re.compile(r'"name":\s*"(\d+\.\d+\.\d+)"')
    # json_strings = response.text.replace('\n', '')[1:-1].split('},  {')
    # json_strings = [obj + '}' if i == 0 else '{' + obj if i == len(json_strings) - 1 else '{' + obj + '}' for
    #                 i, obj in enumerate(json_strings)]
    # json_objects = [json.loads(text) for text in json_strings]
    output = ""
    count = 0
    for obj in json_objects:
        if count < limit and (version is None or obj.get("name").startswith(version)):
            output += 'SonarQube ' + obj.get("name") + '\n'
            count += 1
    click.echo(output)


@click.command()
@click.argument('sq_version', type=str)
def plugins(sq_version):
    """Prints the embedded plugins of a specific SQ version"""
    url = 'http://raw.githubusercontent.com/SonarSource/sonarqube/%s/build.gradle' % sq_version
    print(url)
    output = ""
    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        print("ERROR", response)
        return
    pattern = re.compile(r"dependency\s+'([^']+-plugin[^']*)'")
    for line in response.text.split('\n'):
        match = pattern.search(line)
        if match:
            group = match.group(1).split(':')[1:]
            output += "".join(group) + '\n'
    click.echo(output)
