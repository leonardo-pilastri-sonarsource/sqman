import click

import api.sq_discover as sq_discover
import api.sq_install as sq_install
import api.sq_run as sq_run

@click.group()
def cli():
    pass

cli.add_command(sq_discover.list)
cli.add_command(sq_discover.plugins)
cli.add_command(sq_install.install)
cli.add_command(sq_install.installed)
cli.add_command(sq_install.install_plugin)
cli.add_command(sq_run.run)
cli.add_command(sq_run.status)

if __name__ == '__main__':
    cli()
