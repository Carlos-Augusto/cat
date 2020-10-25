#!/usr/bin/env bash
set -e

VERSION=0.0.4
URL=https://github.com/Carlos-Augusto/cat/releases/download/${VERSION}
INSTALL_FOLDER=/opt/catalina
SYMLINK=/usr/bin/catalina
SYMLINK_HTTP=/usr/bin/catalina-http

if [ $EUID -ne 0 ]; then
  echo "Run with sudo"
  exit 1
fi

if [[ "$(type -t java)" != "file" ]]; then
  echo "No Java found. Please install > 1.8"
  exit 1
fi

mkdir -p ${INSTALL_FOLDER}

function downloadAndUnpack() {
  echo "Downloading and unpacking files from "${URL}
  curl -s ${URL}/catalina-${VERSION}-bin.tar.gz | tar xvz -C ${INSTALL_FOLDER}
}

function removeSymlinks() {
  if [[ -L "${SYMLINK}" ]]; then
    echo "Removing symlink"
    rm ${SYMLINK}
  fi
  if [[ -L "${SYMLINK_HTTP}" ]]; then
    echo "Removing symlink for http"
    rm ${SYMLINK_HTTP}
  fi
}

function install() {
  downloadAndUnpack
  ln -s ${INSTALL_FOLDER}/catalina-${VERSION}/bin/catalina ${SYMLINK}
  ln -s ${INSTALL_FOLDER}/catalina-${VERSION}/bin/catalina-http ${SYMLINK_HTTP}


  echo "-----------"
  echo "Run the CLI:"
  echo " $ catalina"

  echo "-----------"
  echo "Run the HTTP interface:"
  echo " $ catalina-http"
}

function installRemoving() {

  echo "Installing on ${INSTALL_FOLDER} with symlink on ${SYMLINK} and ${SYMLINK_HTTP}"

  removeSymlinks

  if [[ -d "${INSTALL_FOLDER}/catalina-${VERSION}/" ]]; then
    echo "Installation catalina-${VERSION} detected. Removing..."
    rm -r ${INSTALL_FOLDER}/catalina-${VERSION}
  fi

  install

}

function installPrompting() {

  if [[ -d "${INSTALL_FOLDER}/catalina-${VERSION}/" ]]; then
    echo "Installation catalina-${VERSION} detected."
    echo -n "Shall we proceed (y/n)? "
    read -r answer

    if [ "$answer" != "${answer#[Yy]}" ]; then
      removeSymlinks
      rm -r ${INSTALL_FOLDER}/catalina-${VERSION}
    else
      exit 1
    fi

  fi

  installRemoving

}

function help() {
  echo "install [-r] [-p] [-c CAT_HOME] [-e CAT_HOME]"
  echo "-r -> will remove possible existing install"
  echo "-c -> will remove possible existing install on custom place"
  echo "-p -> will prompt if same install is found"
  echo "-e -> will prompt if same install is found and will install on custom place"
}

while getopts "rpc:e:" OPTION; do
  case $OPTION in
  r)
    installRemoving
    exit
    ;;
  c)
    INSTALL_FOLDER=$OPTARG
    installRemoving
    exit
    ;;
  p)
    installPrompting
    exit
    ;;
  e)
    INSTALL_FOLDER=$OPTARG
    installPrompting
    exit
    ;;
  \?)
    help
    exit
    ;;
  esac
done

help
