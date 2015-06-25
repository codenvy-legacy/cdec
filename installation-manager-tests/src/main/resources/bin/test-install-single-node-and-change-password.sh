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

VAGRANT_FILE="../vagrant/single/CentOS70/Vagrantfile"

printAndLog "TEST CASE: Install the latest single-node Codenvy On Premise"

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

# install
ssh -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem 'export TERM="xterm" && bash <(curl -L -s '${PROTOCOL}'://'${SERVER}'/update/repository/public/download/install-codenvy) --silent' >> ${INSTALL_LOG}
validateExitCode $?

auth "admin" "password"

executeIMCommand "im-password" "password" "new-password"
auth "admin" "new-password"

printAndLog "RESULT: PASSED"

