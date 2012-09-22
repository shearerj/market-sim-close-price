#!/bin/bash
# For parsing all observation files stored in a directory (from local runs).
# Include number of new columns to insert in the CSV (parsed from directory name).
if [ $# -ne 3 ]; then
echo "Usage: ./parse_single.sh [Output filename] [Directory of observations] [# columns]"
exit 1
fi

FILES=lib/*.jar
CLASSPATH=.:parser.jar
for i in $FILES
do
  CLASSPATH=${CLASSPATH}:${i}
done

java -cp $CLASSPATH parser.Parser $1 $2 $3
