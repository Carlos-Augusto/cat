#!/usr/bin/env bash
set -e

rm bin -rf
mvn clean package
mkdir bin
cp target/catalina-1.0.0.jar bin/
cp target/lib bin/ -r

