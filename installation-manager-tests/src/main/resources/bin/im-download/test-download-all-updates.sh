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

printAndLog "TEST CASE: Download all updates"

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

log "Latest Codenvy version: "${LATEST_CODENVY_VERSION}
log "Latest IM version: "${LATEST_IM_CLI_CLIENT_VERSION}

installImCliClient ${LATEST_IM_CLI_CLIENT_VERSION}
validateInstalledImCliClientVersion ${LATEST_IM_CLI_CLIENT_VERSION}

executeIMCommand "im-download"

if [[ ! ${OUTPUT} =~ .*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY_VERSION}\".*\"label\".\:.\"RC_UNSTABLE\".*\"file\".\:.\".*codenvy-${LATEST_CODENVY_VERSION}.zip\".*\"status\".\:.\"DOWNLOADED\".*\"status\".\:.\"OK\".* ]]; then
    validateExitCode 1
fi

printAndLog "RESULT: PASSED"

vagrantDestroy
