#!/bin/sh
# JDK mac dinh cua may la 23; Gradle/AGP can 17.
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
cd "$(dirname "$0")" || exit 1
exec ./gradlew "${@:-assembleDebug}"
