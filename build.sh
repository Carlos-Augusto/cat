#!/usr/bin/env bash
set -e
INSTALL_FOLDER=$1

if [ -z "$1" ]
  then
    echo "No install path provided. Please provide where you would like to install the system."
    echo "For example: $HOME/catalina"
    exit 1
fi

echo "Building/Installing in $INSTALL_FOLDER ..."
rm "$INSTALL_FOLDER" -rf
mvn clean package
mkdir "$INSTALL_FOLDER"

cp target/catalina-1.0.0.jar "$INSTALL_FOLDER"
cp target/lib "$INSTALL_FOLDER" -r
touch "$INSTALL_FOLDER"/catalina.sh
echo '#!/usr/bin/env bash' >> "$INSTALL_FOLDER"/catalina.sh
echo 'set -e' >> "$INSTALL_FOLDER"/catalina.sh
echo "cd $INSTALL_FOLDER" >> "$INSTALL_FOLDER"/catalina.sh
echo 'java -cp catalina-1.0.0.jar com.flatmappable.Catalina "$@"' >> "$INSTALL_FOLDER"/catalina.sh
chmod +x "$INSTALL_FOLDER"/catalina.sh

