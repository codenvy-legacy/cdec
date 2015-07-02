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

. ./lib.sh

log "TEST CASE: Get list of downloaded artifacts"

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

installImCliClient
validateInstalledImCliClientVersion

executeIMCommand "im-download"
executeIMCommand "im-download" "--list-local"

if [[ ! ${OUTPUT} =~ .*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY_VERSION}\".*\"label\".\:.\"RC_UNSTABLE\".*\"file\".\:.\".*codenvy-${LATEST_CODENVY_VERSION}.zip\".*\"status\".\:.\"READY_TO_INSTALL\".*\"status\".\:.\"OK\".* ]]; then
    validateExitCode 1
fi

log "RESULT: PASSED"

vagrantDestroy
