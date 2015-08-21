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

UPDATE_SERVER="http://updater-nightly.codenvy-dev.com"
UPDATE_SERVICE="${UPDATE_SERVER}/update"
SAAS_SERVER="https://nightly.codenvy-stg.com"
TEST_LOG="installation-manager-test.log"
SINGLE_NODE_VAGRANT_FILE="../vagrant/single/CentOS71/Vagrantfile"
MULTI_NODE_VAGRANT_FILE="../vagrant/multi/CentOS71/Vagrantfile"
NEW_HOSTNAME="test.codenvy.onprem"

AVAILABLE_CODENVY_VERSIONS=$(curl -s -X GET ${UPDATE_SERVICE}/repository/updates/codenvy)
PREV_CODENVY_VERSION=`echo ${AVAILABLE_CODENVY_VERSIONS} | sed 's/.*"\([^"]*\)","[^"]*"\]/\1/'`
LATEST_CODENVY_VERSION=`echo ${AVAILABLE_CODENVY_VERSIONS} | sed 's/.*"\([^"]*\)".*/\1/'`

AVAILABLE_IM_CLI_CLIENT_VERSIONS=$(curl -s -X GET ${UPDATE_SERVICE}/repository/updates/installation-manager-cli)
PREV_IM_CLI_CLIENT_VERSION=`echo ${AVAILABLE_IM_CLI_CLIENT_VERSIONS} | sed 's/.*"\([^"]*\)","[^"]*"\]/\1/'`
LATEST_IM_CLI_CLIENT_VERSION=`echo ${AVAILABLE_IM_CLI_CLIENT_VERSIONS} | sed 's/.*"\([^"]*\)".*/\1/'`
