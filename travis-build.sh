#!/bin/bash
set -e

export EXIT_STATUS=0

./gradlew clean
./gradlew -Dgeb.env=chromeHeadless complete:test  || EXIT_STATUS=$?

if [[ $EXIT_STATUS -eq 0 ]]; then

    curl -O https://raw.githubusercontent.com/micronaut-projects/micronaut-guides/master/travis/build-guide
    chmod 777 build-guide

    ./build-guide || EXIT_STATUS=$?

    curl -O https://raw.githubusercontent.com/micronaut-projects/micronaut-guides/master/travis/republish-guides-website.sh
    chmod 777 republish-guides-website.sh

    ./republish-guides-website.sh || EXIT_STATUS=$?
fi

exit $EXIT_STATUS
