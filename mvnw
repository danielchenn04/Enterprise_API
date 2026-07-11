#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.

set -e

# Find the project base dir (closest ancestor containing .mvn/)
find_maven_basedir() {
  basedir="$1"
  wdir="$basedir"
  while [ "$wdir" != '/' ]; do
    if [ -d "$wdir/.mvn" ]; then
      basedir="$wdir"
      break
    fi
    wdir=$(cd "$wdir/.." && pwd)
  done
  printf '%s' "$(cd "$basedir" && pwd)"
}

BASE_DIR=$(find_maven_basedir "$(dirname "$0")")

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$BASE_DIR}"
PROPERTIES_FILE="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

# Download wrapper jar if absent
if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL=$(grep '^wrapperUrl' "$PROPERTIES_FILE" | sed 's/wrapperUrl=//')
  echo "Downloading Maven Wrapper from $WRAPPER_URL"
  if command -v curl > /dev/null; then
    curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v wget > /dev/null; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "curl or wget required to download Maven Wrapper" >&2
    exit 1
  fi
fi

# Locate java
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
elif command -v java > /dev/null; then
  JAVACMD="java"
else
  echo "Error: JAVA_HOME is not set and java was not found on PATH." >&2
  exit 1
fi

exec "$JAVACMD" \
  -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" \
  -classpath "$WRAPPER_JAR" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
