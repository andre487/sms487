#!/usr/bin/env python3
import click
import bjoern
from api import app


@click.command()
@click.option('--address', default='0.0.0.0')
@click.option('--port', default=5000)
def main(address: str, port: int):
    bjoern.run(app, address, port)


if __name__ == '__main__':
    main()
