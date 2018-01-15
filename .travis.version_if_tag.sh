#!/bin/bash
if [ -n "$TRAVIS_TAG" ]; then
  VERSION=${TRAVIS_TAG//[^0-9.]/}
  mvn -pl nomer-parent versions:set -DnewVersion=$VERSION
fi
