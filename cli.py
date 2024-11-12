import click
import requests


@click.group()
def cli():
    pass


@click.command()
@click.option('--name', prompt='Your name', help='The person to greet.')
def greet(name):
    """Simple program that greets NAME."""
    click.echo(f'Hello, {name}!')


@click.command()
def list_sq_versions():
    response = requests.get('https://api.github.com/repos/SonarSource/sonarqube/tags?per_page=50')
    click.echo(response.text)


cli.add_command(greet)
cli.add_command(list_sq_versions)

if __name__ == '__main__':
    cli()
