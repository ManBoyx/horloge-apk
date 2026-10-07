#!/bin/sh
# Lance les tests de la logique sans passer par Gradle (quelques secondes).
# Équivalent, plus lent : ./gradlew testReleaseUnitTest
set -e
cd "$(dirname "$0")"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
JUNIT=$(find "$CACHE/junit" -name 'junit-4*.jar' | head -1)
HAMCREST=$(find "$CACHE/org.hamcrest" -name 'hamcrest-core-*.jar' | head -1)
SORTIE=$(mktemp -d)
trap 'rm -rf "$SORTIE"' EXIT
javac -encoding UTF-8 -d "$SORTIE" -cp "$JUNIT:$HAMCREST" \
    $(find app/src/main/java/fr/manboyx/horloge/logique app/src/test -name '*.java')
java -cp "$SORTIE:$JUNIT:$HAMCREST" org.junit.runner.JUnitCore \
    $(find app/src/test -name '*Test.java' | sed 's|app/src/test/java/||; s|\.java$||; s|/|.|g')
