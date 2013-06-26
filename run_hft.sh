#!/bin/bash
if [ $# -lt 1 ]; then
    echo "Usage: ./run_hft.sh [simulation folder] <number of samples to gather : default 1>"
    exit 1
fi
if [[ -z $2 ]]; then
    NUM=1
else
    NUM=$2
fi

echo ">> Building..."
ant
echo ">> Building... done"

CLASSPATH=dist/hft.jar
for i in lib/*.jar; do
  CLASSPATH="${CLASSPATH}:${i}"
done

i=1
while [ $i -le $NUM ]; do
    echo -n ">> Running simulation $i..."
    java -cp "${CLASSPATH}" systemmanager.SystemManager "$1" $i
    i=$((i+1))
    echo " done"
done
