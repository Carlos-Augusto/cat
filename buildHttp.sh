#!/usr/bin/env bash
set -e
INSTALL_FOLDER=$1
VERSION=0.0.4
JARNAME="catalina-$VERSION.jar"

if [ -z "$1" ]
  then
    echo "No install path provided. Please provide where you would like to install the system."
    echo "For example: $HOME/catalina"
    exit 1
fi

echo "Building/Installing in $INSTALL_FOLDER ... version $VERSION"
rm "$INSTALL_FOLDER" -rf
mvn clean package
mkdir "$INSTALL_FOLDER"

cp "target/$JARNAME" "$INSTALL_FOLDER"
cp target/lib "$INSTALL_FOLDER" -r
touch "$INSTALL_FOLDER"/catalina.sh
echo '#!/usr/bin/env bash' >> "$INSTALL_FOLDER"/catalina.sh
echo 'set -e' >> "$INSTALL_FOLDER"/catalina.sh
echo "java -cp $JARNAME"' com.flatmappable.CatalinaHttp' >> "$INSTALL_FOLDER"/catalina.sh
chmod +x "$INSTALL_FOLDER"/catalina.sh

echo "For quick access, add these like to your profile of bashrc file"
echo "export PATH=$INSTALL_FOLDER:\$PATH"

