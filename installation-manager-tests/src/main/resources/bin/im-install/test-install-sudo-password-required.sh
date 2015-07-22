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

printAndLog "TEST CASE: Install when sudo password is required"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

executeSshCommand "sudo sed -i -e 's/vagrant.*/vagrant ALL=\(ALL\) ALL/g' /etc/sudoers"
executeSshCommand "sudo -k"
executeSshCommand "--valid-exit-code=1" "sudo -n -k true"

installCodenvy "--valid-exit-code=1"

printAndLog "RESULT: PASSED"
vagrantDestroy
