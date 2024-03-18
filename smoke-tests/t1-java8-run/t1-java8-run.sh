#!/bin/bash

# Smoke test 1
# run X3D Viewer using Java 8

#requires just JRE:
#sudo aptitude install openjdk-8-jre

/usr/lib/jvm/java-8-openjdk-amd64/bin/java -jar ../../build/libs/x3dview.jar
