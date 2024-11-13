import click

import api.sq_discover as sq_discover
import api.sq_install as sq_install

@click.group()
def cli():
    pass

cli.add_command(sq_discover.list)
cli.add_command(sq_discover.plugins)
cli.add_command(sq_install.install)

if __name__ == '__main__':
    cli()
