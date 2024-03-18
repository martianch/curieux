#!/bin/bash

# Smoke test 3
# run X3D Viewer as a script using Java 13

echo '#''!'/usr/lib/jvm/jdk-13.0.2/bin/java --source 11 | cat - ../../src/main/java/com/github/martianch/curieux/Main.java >x3dview-script
chmod a+x x3dview-script

./x3dview-script

# comment out this to keep the script
rm x3dview-script

