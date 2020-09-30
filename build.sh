#!/usr/bin/env bash
set -e
INSTALL_FOLDER=$1
VERSION=0.0.3

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

cp "target/catalina-${VERSION}-bin.tar.gz" "$INSTALL_FOLDER"
tar -xvf $INSTALL_FOLDER/catalina-${VERSION}-bin.tar.gz -C $INSTALL_FOLDER
rm $INSTALL_FOLDER/catalina-${VERSION}-bin.tar.gz

echo ""
echo "SUCCESS"
echo "For quick access, add these like to your profile of bashrc file"
echo "export PATH=$INSTALL_FOLDER/catalina-$VERSION/bin:\$PATH"

