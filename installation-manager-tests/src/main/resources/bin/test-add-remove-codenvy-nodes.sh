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

printAndLog "TEST CASE: Add and remove Codenvy nodes"
vagrantUp ${MULTI_NODE_VAGRANT_FILE}

installCodenvy
validateInstalledCodenvyVersion

# add runner
executeIMCommand "im-add-node" "runner2.codenvy"
sleep 2m

auth "admin" "password"
doGet "http://codenvy/api/admin/runner/server?token=${TOKEN}"
validateExpectedString ".*http://runner2.codenvy:8080/runner/internal/runner.*"

# add builder
executeIMCommand "im-add-node" "builder2.codenvy"
sleep 2m

auth "admin" "password"
doGet "http://codenvy/api/admin/builder/server?token=${TOKEN}"
validateExpectedString ".*http://builder2.codenvy:8080/builder/internal/builder.*"

# Incorrect name
executeIMCommand "--valid-exit-code=1" "im-add-node" "bla-bla-bla"
validateExpectedString ".*Correct.name.template.is...prefix..number..base_node_domain.*"

# Host is not reachable
executeIMCommand "--valid-exit-code=1" "im-add-node" "builder3.codenvy"
validateExpectedString ".*Can.t.connect.to.host..vagrant@builder3.codenvy:22.*"

# Runner has been already set up
executeIMCommand "--valid-exit-code=1" "im-add-node" "runner2.codenvy"
validateExpectedString ".*Node..runner2.codenvy..has.been.already.used.*"

# remove runner
executeIMCommand "im-remove-node" "runner2.codenvy"
sleep 2m

auth "admin" "password"
doGet "http://codenvy/api/admin/runner/server?token=${TOKEN}"
validateErrorString ".*http://runner2.codenvy:8080/runner/internal/runner.*"

# remove builder
executeIMCommand "im-remove-node" "builder2.codenvy"
sleep 2m

auth "admin" "password"
doGet "http://codenvy/api/admin/builder/server?token=${TOKEN}"
validateErrorString ".*http://builder2.codenvy:8080/builder/internal/builder.*"

# remove already removed runner
executeIMCommand "--valid-exit-code=1" "im-remove-node" "runner2.codenvy"
validateExpectedString ".*Node..runner2.codenvy..is.not.found.*"

printAndLog "RESULT: PASSED"
vagrantDestroy
