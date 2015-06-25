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

printAndLog() {
    echo "$@"
    log "$@"
}

log() {
    echo "$@" >> ${INSTALL_LOG}
}

validateExitCode() {
    EXIT_CODE=$1
    if [[ ! -z ${EXIT_CODE} ]] && [[ ! ${EXIT_CODE} == "0" ]]; then
	    printAndLog "RESULT: FAILED"
#	    vagrantDestroy
        exit ${EXIT_CODE}
    fi
}

vagrantDestroy() {
    vagrant destroy -f >> ${INSTALL_LOG}
}


vagrantUp() {
    VAGRANT_FILE=$1

    cp ${VAGRANT_FILE} Vagrantfile

    vagrant up >> ${INSTALL_LOG}
    validateExitCode $?
}

auth() {
    USERNAME=$1
    PASSWORD=$2

    OUTPUT=$(curl -s -X POST -H "Content-Type: application/json" -d '{"username":"'${USERNAME}'", "password":"'${PASSWORD}'", "realm":"sysldap"}' http://codenvy.onprem/api/auth/login)
    validateExitCode $?

    log ${OUTPUT}

    if [[ ! ${OUTPUT} =~ .*value.* ]]; then
        log ${OUTPUT}
        validateExitCode 1
    fi
}

executeIMCommand() {
    ssh -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem "/home/vagrant/codenvy-im/codenvy-cli/bin/codenvy $@"
    validateExitCode $?
}