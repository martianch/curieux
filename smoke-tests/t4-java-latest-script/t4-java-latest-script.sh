#!/bin/bash

# Smoke test 4
# run X3D Viewer as a script using the latest Java
# at the moment, the latest Java is 21

#echo '#''!'/usr/lib/jvm/java-21-openjdk-amd64/bin/java --source 11 | cat - ../../src/main/java/com/github/martianch/curieux/Main.java >x3dview-script

#The smoke test fails with Java 23. This is a workaround:
mkdir -p com/github/martianch/curieux/
echo '#''!'/usr/lib/jvm/jdk-23-oracle-x64/bin/java --source 11 | cat - ../../src/main/java/com/github/martianch/curieux/Main.java >com/github/martianch/curieux/x3dview-script
echo '#!/bin/bash' >x3dview-script
echo com/github/martianch/curieux/x3dview-script>>x3dview-script
chmod a+x com/github/martianch/curieux/x3dview-script
chmod a+x x3dview-script

./x3dview-script

# comment out this to keep the script
rm x3dview-script
rm -rf com


