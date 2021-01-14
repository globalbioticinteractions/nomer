#!/bin/bash
if [ -n "$TRAVIS_TAG" ]; then
  mvn -s .travis.maven.settings.xml -DskipTests clean deploy
  VERSION=${TRAVIS_TAG//[^0-9.]/}
  cat .travis.jar.magic nomer/target/nomer-${VERSION}-jar-with-dependencies.jar > nomer.jar
fi
