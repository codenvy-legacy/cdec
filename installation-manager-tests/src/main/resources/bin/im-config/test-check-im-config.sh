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

log "Regex validation download.directory property"
[[ ${OUTPUT} =~ .*\"download.directory\".\:.\"/home/vagrant/codenvy-im-data/updates\".* ]] || validateExitCode 1

log "Regex validation update.server.url property"
[[ ${OUTPUT} =~ .*\"update.server.url\".\:.\"${UPDATE_SERVER}\".* ]] || validateExitCode 1

log "Regex validation saas.server.url property"
[[ ${OUTPUT} =~ .*\"saas.server.url\".\:.\"${SAAS_SERVER}\".* ]] || validateExitCode 1

printAndLog "RESULT: PASSED"

vagrantDestroy
