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

printAndLog "TEST CASE: Install unknown CLI version"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

installImCliClient
validateInstalledImCliClientVersion

executeIMCommand "--valid-exit-code=1" "im-install" "codenvy" "1.0.0"

if [[ ! ${OUTPUT} =~ .*Can\'t.download.installation.properties\..*Unexpected.error\..Can\'t.download.the.artifact.codenvy-single-server-properties\:1.0.0\..Artifact.codenvy-single-server-properties\:1.0.0.not.found.* ]]; then
    validateExitCode 1
fi

printAndLog "RESULT: PASSED"
vagrantDestroy
