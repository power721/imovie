#!/usr/bin/env bash

WORK_SPACE='~/git/imovie'
JAR="${WORK_SPACE}/imovie-crawler/target/movie-crawler-0.1.jar"
if [ ! -x "${JAR}" ]; then
  exit 1
fi

if pgrep -f movie-crawler-0.1.jar; then
  exit 1
fi

cd ${WORK_SPACE} || exit 1
java -XX:+HeapDumpOnOutOfMemoryError -jar imovie-crawler/target/movie-crawler-0.1.jar 2>&1 >1 &
