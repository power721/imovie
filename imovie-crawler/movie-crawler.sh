#!/usr/bin/env bash

WORK_SPACE=~/git/imovie
JAR="${WORK_SPACE}/imovie-crawler/target/movie-crawler-0.1.jar"
if [ ! -f "${JAR}" ]; then
  echo >&2 "[$(date)] cannot find ${JAR}"
  exit 1
fi

if pgrep -f movie-crawler-0.1.jar; then
  echo >&2 "[$(date)] crawler is executing"
  exit 1
fi

cd ${WORK_SPACE} || exit 1
echo "[$(date)] executing crawler now"
java -XX:+HeapDumpOnOutOfMemoryError -jar imovie-crawler/target/movie-crawler-0.1.jar >/dev/null 2>&1 &
