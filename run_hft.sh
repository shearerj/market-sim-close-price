#!/bin/bash
if [ $# -ne 2 ]
then
echo "Usage: ./run_hft.sh [simulation folder] [number of samples to gather]"
exit 1
fi

FILES=lib/*.jar
CLASSPATH=.:dist/hft.jar
for i in $FILES
do
  CLASSPATH=${CLASSPATH}:${i}
done

i=1
while [ $i -le $2 ]
do
    java -cp ${CLASSPATH} systemmanager.SystemManager $1 $i
    i=$((i+1))
done
