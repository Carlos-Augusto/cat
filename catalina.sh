#!/usr/bin/env bash
set -e
cd bin
java -cp catalina-1.0.0.jar com.flatmappable.Catalina "$@"

