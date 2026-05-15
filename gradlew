#!/bin/sh

APP_PATH="$0"

while [ -h "$APP_PATH" ]; do
  APP_HOME="${APP_PATH%/*}"
  LINK_TARGET="$(readlink "$APP_PATH")"
  case "$LINK_TARGET" in
    /*) APP_PATH="$LINK_TARGET" ;;
    *) APP_PATH="$APP_HOME/$LINK_TARGET" ;;
  esac
done

APP_HOME="${APP_PATH%/*}"
if [ "$APP_HOME" = "$APP_PATH" ]; then
  APP_HOME="."
fi

APP_BASE_NAME=${0##*/}
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

if [ ! -x "$JAVACMD" ] && ! command -v "$JAVACMD" >/dev/null 2>&1; then
  echo "ERROR: Java is required to run Gradle." >&2
  echo "Set JAVA_HOME or use Android Studio's bundled JDK." >&2
  exit 1
fi

exec "$JAVACMD" \
  -Dorg.gradle.appname="$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
