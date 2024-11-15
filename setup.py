from setuptools import setup, find_packages

setup(
    name='sqman',
    version='0.1.0',
    py_modules=['cli'],
    packages=find_packages(),
    install_requires=[
        'Click',
    ],
    entry_points={
        'console_scripts': [
            'sqman = cli:cli',
        ],
    }
)
