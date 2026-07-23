#!/bin/bash

if [ "$(basename "$(pwd)")" = "scripts" ]; then
  cd ..
fi

echo "Writing ci gradle.properties"
GRADLE_USER_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}"
mkdir -p "$GRADLE_USER_HOME_DIR"
cp ".github/runner-files/ci-gradle.properties" "$GRADLE_USER_HOME_DIR/gradle.properties"