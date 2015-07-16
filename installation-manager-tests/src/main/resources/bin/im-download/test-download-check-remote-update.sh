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

printAndLog "TEST CASE: Check remote update"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

installImCliClient ${PREV_IM_CLI_CLIENT_VERSION}
validateInstalledImCliClientVersion ${PREV_IM_CLI_CLIENT_VERSION}

executeIMCommand "im-download" "--check-remote"
validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY_VERSION}\".*\"status\".\:.\"AVAILABLE_TO_DOWNLOAD\".*"
validateExpectedString ".*\"artifact\".\:.\"installation-manager-cli\".*\"version\".\:.\"${LATEST_IM_CLI_CLIENT_VERSION}\".*\"status\".\:.\"AVAILABLE_TO_DOWNLOAD\".*"

printAndLog "RESULT: PASSED"
vagrantDestroy
