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

installImCliClient
validateInstalledImCliClientVersion

executeIMCommand "im-version"
validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"availableVersion\".*.*\"stable\".\:.\"${LATEST_CODENVY3_VERSION}\".*"

installCodenvy
validateInstalledCodenvyVersion

executeIMCommand "im-version"
validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY3_VERSION}\".*\"label\".\:.\"STABLE\".*\"status\".\:.\"You are running the latest stable version of Codenvy!\".*"

vagrantDestroy

#REPO_DIR="/home/vagrant/codenvy-im-data/updates"
##add new stable version in local repository
#NEW_STABLE_CODENVY_VERSION="117.7.1"
#createDummyArtifactInLocalRepositoryOfIMCli "${REPO_DIR}" codenvy "${NEW_STABLE_CODENVY_VERSION}" "${LATEST_CODENVY3_VERSION}" STABLE
#
#executeIMCommand "im-version"
#validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY3_VERSION}\".*\"label\".\:.\"STABLE\".*\"availableVersion\".*\"stable\".\:.\"${NEW_STABLE_CODENVY_VERSION}\".*\"status\".\:.\"There is a new stable version of Codenvy available. Run im-download ${NEW_STABLE_CODENVY_VERSION}.\".*"
#
##add new unstable version in local repository
#NEW_UNSTABLE_CODENVY_VERSION="117.7.2"
#createDummyArtifactInLocalRepositoryOfIMCli "${REPO_DIR}" codenvy "${NEW_UNSTABLE_CODENVY_VERSION}" "${LATEST_CODENVY3_VERSION}" UNSTABLE
#
#executeIMCommand "im-version"
#validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY3_VERSION}\".*\"label\".\:.\"STABLE\".*\"availableVersion\".*\"stable\".\:.\"${NEW_STABLE_CODENVY_VERSION}\".*\"unstable\".\:.\"${NEW_STABLE_CODENVY_VERSION}\".*\"status\".\:.\"There is a new stable version of Codenvy available. Run im-download ${NEW_STABLE_CODENVY_VERSION}.\".*"
#
##remove new stable version in local repository
#removeArtifactInLocalRepositoryOfIMCli "${REPO_DIR}" codenvy "117.7.1"
#NEW_UNSTABLE_CODENVY_VERSION="117.7.2"
#
#executeIMCommand "im-version"
#validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY3_VERSION}\".*\"label\".\:.\"STABLE\".*\"availableVersion\".*\"unstable\".\:.\"${NEW_UNSTABLE_CODENVY_VERSION}\".*\"status\".\:.\"You are running the latest stable version of Codenvy!\".*"
#
##remove new unstable version in local repository
#removeArtifactInLocalRepositoryOfIMCli "${REPO_DIR}" codenvy "$117.7.2"
#
##add new stable version in local repository
#NEW_STABLE_CODENVY_VERSION="117.7.3"
#createDummyArtifactInLocalRepositoryOfIMCli "${REPO_DIR}" codenvy "${NEW_STABLE_CODENVY_VERSION}" "${LATEST_CODENVY3_VERSION}" STABLE
#
##change label to "UNSTABLE" for codenvy ${LATEST_CODENVY3_VERSION} in local repository
#changePropertyOfArtifactInLocalRepositoryOfIMCli "${REPO_DIR}" codenvy "${LATEST_CODENVY3_VERSION}" "label=STABLE" "label=UNSTABLE"
#
#executeIMCommand "im-version"
#validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY3_VERSION}\".*\"label\".\:.\"UNSTABLE\".*\"availableVersion\".*\"stable\".\:.\"${NEW_STABLE_CODENVY_VERSION}\".*\"status\".\:.\"There is a new stable version of Codenvy available. Run im-download ${NEW_STABLE_CODENVY_VERSION}.\".*"
