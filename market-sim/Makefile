COLOR = sed -E -e "/^\\[ERROR\\]/{s/^/`tput setaf 1`/;s/\$$/`tput sgr0`/}" -e "/^\\[WARNING\\]/{s/^/`tput setaf 3`/;s/\$$/`tput sgr0`/}" -e "/BUILD FAILURE$$/{s/^/`tput setaf 1`/;s/\$$/`tput sgr0`/}" -e "/BUILD SUCCESS$$/{s/^/`tput setaf 2`/;s/\$$/`tput sgr0`/}" -e "/^Tests run: ([0-9]+), Failures: 0, Errors: 0, Skipped: \1/{s/^/`tput dim`/;s/\$$/`tput sgr0`/}" -e "/^Tests run: [0-9]+, Failures: 0, Errors: 0/{s/^/`tput setaf 2`/;s/\$$/`tput sgr0`/}" -e "/^Tests run: [0-9]+/{/Failures: 0, Errors: 0/!{s/^/`tput setaf 1`/;s/\$$/`tput sgr0`/};}"



jar:
	@echo mvn compile assembly:single
	@mvn compile assembly:single | $(COLOR)

test:
	@echo mvn test
	@mvn test | $(COLOR)

report:
	@echo mvn site
	@mvn site | $(COLOR)
	sensible-browser target/site/project-reports.html 

docs: FORCE
	$(MAKE) -C docs

FORCE:
