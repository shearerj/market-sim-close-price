"""Install script for setuptools."""
from __future__ import absolute_import, division, print_function

from setuptools import setup

setup(
    name="market_manipulation",
    version="0.0.1",
    description="Market-Sim + DRL",
    packages=["market_manipulation"],
    classifiers=[
        "Environment :: Console",
        "Intended Audience :: Science/Research",
        "Operating System :: POSIX :: Linux",
        "Operating System :: Microsoft :: Windows",
        "Operating System :: MacOS :: MacOS X",
        "Programming Language :: Python :: 3.7",
        "Topic :: Scientific/Engineering :: Artificial Intelligence",
    ],
)
