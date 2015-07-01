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

printAndLog "TEST CASE: Install and update IM CLI client"

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

log "Available IM versions: "${AVAILABLE_IM_CLI_CLIENT_VERSIONS}
log "Previos IM version: "${PREV_IM_CLI_CLIENT_VERSION}
log "Latest IM version: "${LATEST_IM_CLI_CLIENT_VERSION}

installImCliClient ${PREV_IM_CLI_CLIENT_VERSION}
validateInstalledImCliClientVersion ${PREV_IM_CLI_CLIENT_VERSION}

executeIMCommand "im-download" "installation-manager-cli" "${LATEST_IM_CLI_CLIENT_VERSION}"
executeIMCommand "im-install" "installation-manager-cli" "${LATEST_IM_CLI_CLIENT_VERSION}"
executeIMCommand "help" # just to update itself

validateInstalledImCliClientVersion ${LATEST_IM_CLI_CLIENT_VERSION}

printAndLog "RESULT: PASSED"

vagrantDestroy
