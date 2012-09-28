#!/bin/bash
# For parsing all observation files stored in a directory (from local runs).
if [ $# -ne 2 ]; then
echo "Usage: ./parse_single.sh [Output filename] [Directory of observations]"
exit 1
fi

FILES=lib/*.jar
CLASSPATH=.
for i in $FILES
do
  CLASSPATH=${CLASSPATH}:${i}
done

java -cp $CLASSPATH parser.Parser $1 ${2%/}
