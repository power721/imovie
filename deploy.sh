#!/usr/bin/env bash

echo "try to update code"
git pull || exit 1
echo "build artifacts"
mvn clean install -DskipTests=true || exit 1
echo "copy artifact to /opt/imovies"
cp imovie-web/target/imovie-web-0.1.jar /opt/imovies || exit 1
echo "restart service"
sudo service imovie restart
sudo service imovie status