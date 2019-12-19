#!/bin/bash

# build multiple versions by changing application.properties
PROPERTIES_FILE=src/main/resources/application.properties

replaceProperties() {
    delayInMillis=$1
    version=$2
    promotionRate=$3

    sed -i "s/\(delayInMillis=\).*\$/\1${delayInMillis}/" $PROPERTIES_FILE
    sed -i "s/\(version=\).*\$/\1${version}/" $PROPERTIES_FILE
    sed -i "s/\(promotionRate=\).*\$/\1${promotionRate}/" $PROPERTIES_FILE
}

# v1: no slowdown, no promotion rate

replaceProperties "v1" "0" "0"
mvn package
docker build . -t "${IMAGE}:${GIT_SHA}.1"

# v2: slowdown, no promotion rate

replaceProperties "v2" "1000" "0"
mvn package
docker build . -t "${IMAGE}:${GIT_SHA}.2"

# v3: no slowdown, no promotion rate

replaceProperties "v3" "0" "0"
mvn package
docker build . -t "${IMAGE}:${GIT_SHA}.3"
