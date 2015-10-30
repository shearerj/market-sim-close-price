# Make file for building and doing most things of use with the market simulator
# usage: see `make help`

# Color the output of maven with long but simple sed script
COLOR = sed -E -e "/^\\[ERROR\\]/{s/^/`tput setaf 1`/;s/\$$/`tput sgr0`/}" -e "/^\\[WARNING\\]/{s/^/`tput setaf 3`/;s/\$$/`tput sgr0`/}" -e "/BUILD FAILURE$$/{s/^/`tput setaf 1`/;s/\$$/`tput sgr0`/}" -e "/BUILD SUCCESS$$/{s/^/`tput setaf 2`/;s/\$$/`tput sgr0`/}" -e "/^Tests run: ([0-9]+), Failures: 0, Errors: 0, Skipped: \1/{s/^/`tput dim`/;s/\$$/`tput sgr0`/}" -e "/^Tests run: [0-9]+, Failures: 0, Errors: 0/{s/^/`tput setaf 2`/;s/\$$/`tput sgr0`/}" -e "/^Tests run: [0-9]+/{/Failures: 0, Errors: 0/!{s/^/`tput setaf 1`/;s/\$$/`tput sgr0`/};}"

# Current directory
PWD := $(shell pwd)

# Set error for egta target of `def` is not defined
ifndef def
	ERROR = $(error `def` is undefined. Make sure you build egta like \
		`make egta def=<default.json>`. See `make help`)
endif

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
	@echo "  docs   : Compile docmentation"
	@echo "  clean  : Remove all java byproducts"

jar:
	@echo mvn compile assembly:single
	@mvn compile assembly:single | $(COLOR)

egta:
	$(ERROR)
	$(eval $@_TMP := $(shell mktemp -d))
	$(eval $@_NAME := $(notdir $(basename $(def))))
	mkdir -p "$($@_TMP)/$($@_NAME)/script/target"
	ln -s "$(PWD)/$(def)" "$($@_TMP)/$($@_NAME)/defaults.json"
	ln -s "$(PWD)/target/marketsim-4.0.0-jar-with-dependencies.jar" "$($@_TMP)/$($@_NAME)/script/target/"
	ln -s "$(PWD)/run-egta.sh" "$($@_TMP)/$($@_NAME)/script/batch"
	cd $($@_TMP) && zip -r "$(PWD)/$($@_NAME).zip" "$($@_NAME)"
	rm -rf "$($@_TMP)"

test:
	@echo mvn test
	@mvn test | $(COLOR)

report:
	@echo mvn site
	@mvn site | $(COLOR)
	sensible-browser target/site/project-reports.html 

docs:
	$(MAKE) -C docs

clean:
	@echo mvn clean
	@mvn clean | $(COLOR)

.PHONY: docs
