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

. ./config.sh

trap cleanUp EXIT

cleanUp() {
    vagrantDestroy
}

printAndLog() {
    echo "$@"
    log "$@"
}

log() {
    echo "$@" >> ${TEST_LOG}
}

validateExitCode() {
    EXIT_CODE=$1
    if [[ ! -z ${EXIT_CODE} ]] && [[ ! ${EXIT_CODE} == "0" ]]; then
	    printAndLog "RESULT: FAILED"
	    retrieveInstallLog
	    vagrantDestroy
        exit ${EXIT_CODE}
    fi
}

vagrantDestroy() {
    echo
    vagrant destroy -f >> ${TEST_LOG}
}

retrieveInstallLog() {
    scp -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem:install.log tmp.log
    if [ -f "tmp.log" ]; then
        if [ -f "install.log" ]; then
            cat tmp.log >> install.log
        else
            cp tmp.log install.log
        fi
        rm tmp.log
    fi
}

installCodenvy() {
    ssh -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem 'export TERM="xterm" && bash <(curl -L -s '${UPDATE_SERVER}'/repository/public/download/install-codenvy) --silent' >> ${TEST_LOG}
    validateExitCode $?
}

vagrantUp() {
    VAGRANT_FILE=$1

    cp ${VAGRANT_FILE} Vagrantfile

    vagrant up >> ${TEST_LOG}
    validateExitCode $?
}

auth() {
    log
    log "TEST: authentication"

    USERNAME=$1
    PASSWORD=$2

    OUTPUT=$(curl -s -X POST -H "Content-Type: application/json" -d '{"username":"'${USERNAME}'", "password":"'${PASSWORD}'", "realm":"sysldap"}' http://codenvy.onprem/api/auth/login)
    validateExitCode $?

    log ${OUTPUT}

    if [[ ! ${OUTPUT} =~ .*value.* ]]; then
        validateExitCode 1
    fi
}

executeIMCommand() {
    log
    log "TEST: executing command "$@

    ssh -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem "/home/vagrant/codenvy-im/codenvy-cli/bin/codenvy $@" >> ${TEST_LOG}
    validateExitCode $?
}