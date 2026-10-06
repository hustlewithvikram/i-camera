#!/bin/sh

# Gradle Wrapper bootstrap for iCamera.
# The wrapper JAR is intentionally not required; CI and Android Studio pin Gradle
# through gradle-wrapper.properties. If a system Gradle is available, use it.

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "Gradle is not installed on PATH."
echo "Open the project in Android Studio and use its configured Gradle 8.13 distribution,"
echo "or install Gradle 8.13 and run this script again."
exit 1
