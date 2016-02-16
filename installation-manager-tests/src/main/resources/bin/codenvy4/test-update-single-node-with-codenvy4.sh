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

printAndLog "TEST CASE: Update previous version of single-node Codenvy 4.x On Premise to latest version"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

# install previous version
installCodenvy ${PREV_CODENVY4_VERSION}
validateInstalledCodenvyVersion ${PREV_CODENVY4_VERSION}

# make backup
executeIMCommand "im-backup"
fetchJsonParameter "file"
BACKUP_PATH=${OUTPUT}

# update to latest version
executeIMCommand "im-download" "codenvy" "${LATEST_CODENVY4_VERSION}"
executeIMCommand "im-install" "codenvy" "${LATEST_CODENVY4_VERSION}"
validateInstalledCodenvyVersion ${LATEST_CODENVY4_VERSION}

# should be an error when try to restore from backup of another version
executeIMCommand "--valid-exit-code=1" "im-restore" ${BACKUP_PATH}
validateExpectedString ".*\"Version.of.backed.up.artifact.'${PREV_CODENVY4_VERSION}'.doesn't.equal.to.restoring.version.'${LATEST_CODENVY4_VERSION}'\".*\"status\".\:.\"ERROR\".*"

printAndLog "RESULT: PASSED"
vagrantDestroy
