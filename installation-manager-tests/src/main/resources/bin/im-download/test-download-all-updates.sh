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

printAndLog "TEST CASE: Download all updates"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

installImCliClient
validateInstalledImCliClientVersion

executeIMCommand "im-download"
validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_STABLE_CODENVY_VERSION}\".*\"file\".\:.\".*codenvy-${LATEST_STABLE_CODENVY_VERSION}.zip\".*\"status\".\:.\"DOWNLOADED\".*"

executeIMCommand "im-download" "--list-local"
validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_STABLE_CODENVY_VERSION}\".*\"file\".\:.\".*codenvy-${LATEST_STABLE_CODENVY_VERSION}.zip\".*\"status\".\:.\"READY_TO_INSTALL\".*"

executeIMCommand "--valid-exit-code=1" "im-download" "unknown"
executeIMCommand "--valid-exit-code=1" "im-download" "codenvy" "1.0.0"

printAndLog "RESULT: PASSED"
vagrantDestroy
