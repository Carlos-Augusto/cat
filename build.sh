#!/usr/bin/env bash
set -e

echo 'Building ...'
rm bin -rf
mvn clean package
mkdir bin

echo 'Installing ...'
cp target/catalina-1.0.0.jar bin/
cp target/lib bin/ -r
touch bin/catalina.sh
echo '#!/usr/bin/env bash' >> bin/catalina.sh
echo 'set -e' >> bin/catalina.sh
echo 'cd $HOME/sources/catalina/bin' >> bin/catalina.sh
echo 'java -cp catalina-1.0.0.jar com.flatmappable.Catalina "$@"' >> bin/catalina.sh
chmod +x bin/catalina.sh

