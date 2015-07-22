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

printAndLog "TEST CASE: Install and update IM CLI client"

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

installImCliClient ${PREV_IM_CLI_CLIENT_VERSION}

# test auto-update at the start of executing some command
executeIMCommand "im-download" "-c"
validateExpectedString ".*This.CLI.client.was.out-dated.so.automatic.update.has.being.started\..It.will.be.finished.at.the.next.launch.*"

validateInstalledImCliClientVersion ${LATEST_IM_CLI_CLIENT_VERSION}
validateExpectedString ".*Installation.Manager.CLI.is.being.updated.\.\.\..*"

printAndLog "RESULT: PASSED"

vagrantDestroy
