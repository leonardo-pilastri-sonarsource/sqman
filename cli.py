import click

from api.sq_discover import *


@click.group()
def cli():
    pass


@click.command()
@click.option('--name', prompt='Your name', help='The person to greet.')
def greet(name):
    """Simple program that greets NAME."""
    click.echo(f'Hello, {name}!')


cli.add_command(greet)
cli.add_command(list)
cli.add_command(plugins)

if __name__ == '__main__':
    cli()
