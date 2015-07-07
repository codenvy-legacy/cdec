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

printAndLog "TEST CASE: Connect to update server when there is system proxy"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

# Install and configure proxy-server Squid on port 3128:
executeSshCommand "sudo yum install squid -y -q"
executeSshCommand "sudo squid start"
executeSshCommand "sudo chkconfig --levels 235 squid on"

# Setup system proxy parameters
executeSshCommand "sed -i '2iexport http_proxy=http://127.0.0.1:3128/' ~/.bashrc"
executeSshCommand "sed -i '2iexport https_proxy=http://127.0.0.1:3128/' ~/.bashrc"

installImCliClient
validateInstalledImCliClientVersion

executeIMCommand "im-download" "--check-remote"

if [[ ! ${OUTPUT} =~ .*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY_VERSION}\".*\"status\".\:.\"AVAILABLE_TO_DOWNLOAD\".*\"status\".\:.\"OK\".* ]]; then
    validateExitCode 1
fi

# Ensure, there is record with info about request in the log of Squid proxy-server
executeSshCommand "sudo grep \"GET ${UPDATE_SERVICE}\" /var/log/squid/access.log"

printAndLog "RESULT: PASSED"
vagrantDestroy
