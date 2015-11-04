#!/bin/bash
#
# CODENVY CONFIDENTIAL
# ________________
#
# [2012] - [2015] Codenvy, S.A.
# All Rights Reserved.
# NOTICE: All information contained herein is, and remains
# the property of Codenvy S.A. and its suppliers,
# if any. The intellectual and technical concepts contained
# herein are proprietary to Codenvy S.A.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
# Dissemination of this information or reproduction of this material
# is strictly forbidden unless prior written permission is obtained
# from Codenvy S.A..
#

[ -f "./lib.sh" ] && . ./lib.sh
[ -f "../lib.sh" ] && . ../lib.sh

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

REPO_DIR="/home/vagrant/codenvy-im-data/updates"

installImCliClient
validateInstalledImCliClientVersion

printAndLog "TEST CASE 1: Check im-version command when Codenvy On-Prem didn't installed and available stable version"
executeIMCommand "im-version"
validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"availableVersion\".*.*\"stable\".\:.\"${LATEST_CODENVY_VERSION}\".*"
printAndLog "RESULT 1: PASSED"

installCodenvy
validateInstalledCodenvyVersion

printAndLog "TEST CASE 2: Check im-version command when Codenvy On-Prem installed latest stable version and status latest stable version installed"
executeIMCommand "im-version"
validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY_VERSION}\".*\"label\".\:.\"STABLE\".*\"status\".\:.\"You are running the latest stable version of Codenvy!\".*"
printAndLog "RESULT 2: PASSED"

#printAndLog "TEST CASE 3: Check im-version command when Codenvy On-Prem instaled and available vew stable version and status new stable version available"
##add new stable varsion in local repository
#NEW_STABLE_CODENVY_VERSION="117.7.1"
#createDummyArtifactInLocalReposiotryOfIMCli "${REPO_DIR}" codenvy "${NEW_STABLE_CODENVY_VERSION}" "${LATEST_CODENVY_VERSION}" STABLE
#
#executeIMCommand "im-version"
#validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY_VERSION}\".*\"label\".\:.\"STABLE\".*\"availableVersion\".*\"stable\".\:.\"${NEW_STABLE_CODENVY_VERSION}\".*\"status\".\:.\"There is a new stable version of Codenvy available. Run im-download ${NEW_STABLE_CODENVY_VERSION}.\".*"
#printAndLog "RESULT 3: PASSED"
#
#printAndLog "TEST CASE 4: Check im-version command when Codenvy On-Prem installed and available new stable and unstable versions and status new stable version available"
##add new unstable varsion in local repository
#NEW_UNSTABLE_CODENVY_VERSION="117.7.2"
#createDummyArtifactInLocalReposiotryOfIMCli "${REPO_DIR}" codenvy "${NEW_UNSTABLE_CODENVY_VERSION}" "${LATEST_CODENVY_VERSION}" UNSTABLE
#
#executeIMCommand "im-version"
#validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY_VERSION}\".*\"label\".\:.\"STABLE\".*\"availableVersion\".*\"stable\".\:.\"${NEW_STABLE_CODENVY_VERSION}\".*\"unstable\".\:.\"${NEW_STABLE_CODENVY_VERSION}\".*\"status\".\:.\"There is a new stable version of Codenvy available. Run im-download ${NEW_STABLE_CODENVY_VERSION}.\".*"
#printAndLog "RESULT 4: PASSED"
#
#printAndLog "TEST CASE 5: Check im-version command when Codenvy On-Prem installed and available unstable version and status latest stable version installed"
##remove new stable varsion in local repository
#removeArtifactInLocalReposiotryOfIMCli "${REPO_DIR}" codenvy "117.7.1"
#NEW_UNSTABLE_CODENVY_VERSION="117.7.2"
#
#executeIMCommand "im-version"
#validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY_VERSION}\".*\"label\".\:.\"STABLE\".*\"availableVersion\".*\"unstable\".\:.\"${NEW_UNSTABLE_CODENVY_VERSION}\".*\"status\".\:.\"You are running the latest stable version of Codenvy!\".*"
#printAndLog "RESULT 5: PASSED"
#
#printAndLog "TEST CASE 6: Check im-version command when Codenvy On-Prem installed unstable version and and available stable version and status new stable version available"
##remove new unstable varsion in local repository
#removeArtifactInLocalReposiotryOfIMCli "${REPO_DIR}" codenvy "$117.7.2"
#
##add new stable varsion in local repository
#NEW_STABLE_CODENVY_VERSION="117.7.3"
#createDummyArtifactInLocalReposiotryOfIMCli "${REPO_DIR}" codenvy "${NEW_STABLE_CODENVY_VERSION}" "${LATEST_CODENVY_VERSION}" STABLE
#
##change lable to "UNSTABLE" for codenvy ${LATEST_CODENVY_VERSION} in local repository
#changePropertyOfArtifactInLocalReposiotryOfIMCli "${REPO_DIR}" codenvy "${LATEST_CODENVY_VERSION}" "label=STABLE" "label=UNSTABLE"
#
#executeIMCommand "im-version"
#validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY_VERSION}\".*\"label\".\:.\"UNSTABLE\".*\"availableVersion\".*\"stable\".\:.\"${NEW_STABLE_CODENVY_VERSION}\".*\"status\".\:.\"There is a new stable version of Codenvy available. Run im-download ${NEW_STABLE_CODENVY_VERSION}.\".*"
#printAndLog "RESULT 6: PASSED"

vagrantDestroy
