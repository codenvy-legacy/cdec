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

vagrantUp ${MULTI_NODE_VAGRANT_FILE}

printAndLog "TEST CASE: Add and remove Codenvy nodes"

installCodenvy
validateInstalledCodenvyVersion

auth "admin" "password"

# add runner
executeIMCommand "im-add-node" "runner2.codenvy.onprem"
RUNNERS=`curl 'http://codenvy.onprem/api/admin/runner/server?token='${TOKEN}`
[[ ! ${RUNNERS} =~ .*http://runner2.codenvy.onprem:8080/runner/internal/runner.* ]] && validateExitCode 1

# add builder
executeIMCommand "im-add-node" "builder2.codenvy.onprem"
BUILDERS=`curl 'http://codenvy.onprem/api/admin/builder/server?token='${TOKEN}`
[[ ! ${BUILDERS} =~ .*http://builder2.codenvy.onprem:8080/builder/internal/builder.* ]] && validateExitCode 1

# Incorrect name
executeIMCommand "--valid-exit-code=1" "im-add-node" "bla-bla-bla"
[[ ! ${OUTPUT} =~ .*Correct.name.template.is..\<prefix\>\<number\>\<base_node_domain\>..* ]] && validateExitCode 1

# Host is not reachiable
executeIMCommand "--valid-exit-code=1" "im-add-node" "builder3.codenvy.onprem"
[[ ! ${OUTPUT} =~ .*Can.t.connect.to.host..vagrant@builder3.codenvy.onprem:22.*socket.is.not.established.* ]] && validateExitCode 1

# Runner has been already set up
executeIMCommand "--valid-exit-code=1" "im-add-node" "runner2.codenvy.onprem"
[[ ! ${OUTPUT} =~ .*Node..runner2.codenvy.onprem..has.been.already.used.* ]] && validateExitCode 1

# remove runner
executeIMCommand "im-remove-node" "runner2.codenvy.onprem"
RUNNERS=`curl 'http://codenvy.onprem/api/admin/runner/server?token='${TOKEN}`
[[ ${RUNNERS} =~ .*http://runner2.codenvy.onprem:8080/runner/internal/runner.* ]] && validateExitCode 1

# remove builder
executeIMCommand "im-remove-node" "builder2.codenvy.onprem"
BUILDERS=`curl 'http://codenvy.onprem/api/admin/builder/server?token='${TOKEN}`
[[ ${BUILDERS} =~ .*http://builder2.codenvy.onprem:8080/builder/internal/builder.* ]] && validateExitCode 1

# remove already removed runner
executeIMCommand "--valid-exit-code=1" "im-remove-node" "runner2.codenvy.onprem"
[[ ! ${OUTPUT} =~ .*Node..runner2.codenvy.onprem..is.not.found.* ]] && validateExitCode 1

printAndLog "RESULT: PASSED"
vagrantDestroy
