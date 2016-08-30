SRC=$(wildcard *.md)
PDF=$(SRC:.md=.pdf)

all: $(PDF)

%.pdf: %.md
	pandoc $^ -o $@
