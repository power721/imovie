#!/usr/bin/env bash

git pull || exit 1
mvn clean install -DskipTests=true || exit 1
cp imovie-web/target/imovie-web-0.1.jar /opt/imovies || exit 1
sudo service imovie restart