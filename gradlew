#!/usr/bin/env sh
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVA_OPTS=""
GRADLE_OPTS=""
for i in "$@" ; do
    case "$i" in
        -D*) JAVA_OPTS="$JAVA_OPTS $i" ;;
        *) ;;
    esac
done
exec "$JAVACMD" $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
