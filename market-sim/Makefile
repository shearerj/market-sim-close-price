# Make file for building and doing most things of use with the market simulator
# usage: see `make help`
SHELL=/usr/bin/env bash -euf -o pipefail

# Color the output of maven with long but simple sed script
COLOR = sed -Ef resources/mvn_color.sed

# Current directory
PWD := $(shell pwd)

# Set error for egta target of `def` is not defined
ifndef def
	ERROR = $(error `def` is undefined. Make sure you build egta like \
		`make egta def=<default.json>`. See `make help`)
endif

KEYS=$(wildcard target/site/apidocs/edu/umich/srg/marketsim/Keys.*.html)

## Targets
help:
	@echo "usage: make <target>"
	@echo "where <target> is one of:"
	@echo "  jar    : Compile java into an executable jar that can be called with"
	@echo "           market-sim.sh or run-egta.sh"
	@echo "  test   : Run all java unit tests"
	@echo "  egta def=<defaults.json> : Compile an egta zip file, where defaults.json"
	@echo "           is the location of the desired defaults.json file, and the name"
	@echo "           of the file is the simulator name"
	@echo "  report : Generate and display a number of reports about the java code"
	@echo "  docs   : Compile docmentation any"
	@echo "  clean  : Remove all java byproducts"

jar:
	@echo mvn compile assembly:single
	@mvn compile assembly:single | $(COLOR)

egta: jar
	$(ERROR)
	$(eval $@_TMP := $(shell mktemp -d))
	$(eval $@_NAME := $(notdir $(basename $(def))))
	mkdir -p "$($@_TMP)/$($@_NAME)/script/target"
	mkdir -p "$($@_TMP)/$($@_NAME)/script/jq"
	ln -s "$(PWD)/$(def)" "$($@_TMP)/$($@_NAME)/defaults.json"
	ln -s "$(PWD)/target/marketsim-4.0.0-jar-with-dependencies.jar" "$($@_TMP)/$($@_NAME)/script/target/"
	ln -s "$(PWD)/jq/merge_payoffs.jq" "$($@_TMP)/$($@_NAME)/script/jq"
	ln -s "$(PWD)/run-egta.sh" "$($@_TMP)/$($@_NAME)/script/batch"
	cd $($@_TMP) && zip -r "$(PWD)/$($@_NAME).zip" "$($@_NAME)"
	rm -rf "$($@_TMP)"

test:
	@echo mvn test
	@mvn test | $(COLOR)

create-report:
	@echo mvn site
	@mvn site | $(COLOR)

report: create-report
	sensible-browser target/site/project-reports.html 

docs: ../docs/keys.md

../docs/keys.md: $(KEYS)
	mvn javadoc:javadoc | $(COLOR)
	node scripts/keys_docs.js $^ > $@

clean:
	@echo mvn clean
	@mvn clean | $(COLOR)

.PHONY: docs
