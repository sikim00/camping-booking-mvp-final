#!/usr/bin/env sh
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "[ERROR] gradle-wrapper.jar not found: $WRAPPER_JAR" >&2
  exit 1
fi
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  exec "$JAVA_HOME/bin/java" -jar "$WRAPPER_JAR" "$@"
else
  exec java -jar "$WRAPPER_JAR" "$@"
fi
