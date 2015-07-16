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

printAndLog "TEST CASE: Check current installation-manager config"

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

installImCliClient
validateInstalledImCliClientVersion

executeIMCommand "im-config"

validateExpectedString ".*\"download.directory\".\:.\"/home/vagrant/codenvy-im-data/updates\".*"
validateExpectedString ".*\"update.server.url\".\:.\"${UPDATE_SERVER}\".*"
validateExpectedString ".*\"saas.server.url\".\:.\"${SAAS_SERVER}\".*"

printAndLog "RESULT: PASSED"

vagrantDestroy
