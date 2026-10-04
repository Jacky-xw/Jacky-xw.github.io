#!/bin/sh

# Gradle Wrapper launcher. The checked-in wrapper JAR reads
# gradle/wrapper/gradle-wrapper.properties and downloads the pinned
# distribution when it is not cached on the build machine.

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "${JAVA_HOME:-}" ]; then
    JAVA_EXEC="$JAVA_HOME/bin/java"
else
    JAVA_EXEC=java
fi

if [ ! -x "$JAVA_EXEC" ] && [ "$JAVA_EXEC" != "java" ]; then
    echo "JAVA_HOME does not point to an executable Java 17 installation: $JAVA_EXEC" >&2
    exit 1
fi

exec "$JAVA_EXEC" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
