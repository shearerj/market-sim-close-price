% Developing with the Market Simulator

Overview
========

Quick Start
===========

This is a quick start guide to checkout and run the repository. These commands
should be vaguely understood before executing them.

```
sudo apt-get install openjdk-8-jdk make maven jq git
ssh-keygen
# Do: Email elaine ~/.ssh/id_rsa.pub
git clone git@hft.eecs.umich.edu:hft.git
cd hft/market-sim
make jar
# This next line executes one run of a default simulation
< docs/simulation_spec_condensed.json ./market-sim.sh 1 | jq .
# This next line creates an egta simulation called defaults.zip
make egta def=docs/defaults.json
```

Dependencies
============

Below is a list of all of the dependencies followed by their name in standard
Ubuntu repositories.

1. Java 1.8 (`openjdk-8-jdk`)
2. Make (`make`)
3. Maven 3 (`maven`)
4. Jq (`jq`)
5. Git (`git`)

Compiling & Common Tasks
========================

Make
----

There is a Makefile at the root of market-sim that provides targets for most
operations you would want to accomplish. Typing `make help` will display
information about all of them.

jar
  : Compile java into an executable jar that can be called with
    `market-sim.sh`, or `run-egta.sh` This essentially updates the current
    executable simulator with the current version in source.

test
  : Run all java unit tests. Make sure that this passes before merging code
    into a larger branch.

egta def=\<defaults.json\>
  : Compile the current jar into an egta compatible zip file. This requires
    that `def` be specified. `def` is the path to the egta `defaults.json` you
    wish to use for this jar. It will also be used for the name of the
    resulting zip file, so make sure that the name of the `defaults.json`
    you're using coincides with the desored simulator name.

report
  : Generate and display a number of reports about the java source code. In
    general this should be free of most of the errors is points out. This
    should be run before a large code change.

docs
  : Compile the documentation markdown files into pdfs for easier viewing.

clean
  : Remove all java byproducts. This probably won't be necessary, but it exists
  just in case.

To remove the syntax highlighting add `COLOR=cat` to any command invocation.

Style
=====

We currently use the [Google Style
Guide](http://google.github.io/styleguide/javaguide.html). In the `resouces`
folder there are two files (`eclipse-java-google-style.xml` and
`google-style.importorder`) to help enforce this style in Eclipse.
