#!/bin/bash

mvn package -DskipTests
docker build . -t jetzlstorfer/carts:0.10.4 && docker push jetzlstorfer/carts:0.10.4