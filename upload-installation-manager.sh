#
# CODENVY CONFIDENTIAL
# ________________
#
# [2012] - [2014] Codenvy, S.A.
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

#!/bin/bash
if [ -z "$1" ] || [ "$1" == "prod" ]; then
    SSH_KEY_NAME=cl-server-prod-20130219
    SSH_AS_USER_NAME=codenvy
    AS_IP=update.codenvycorp.com
    echo "Uploading on production"
elif [ "$1" == "stg" ]; then
    SSH_KEY_NAME=as1-cldide_cl-server.skey
    SSH_AS_USER_NAME=codenvy
    AS_IP=syslog.codenvy-stg.com
    AS_IP=git_nopass.key
    echo "Uploading on staging"
else
    echo "Unknown server destination"
    exit
fi

doUpload() {
    DESTINATION=update-server-repository/${ARTIFACT}/${VERSION}

    echo "file=${FILENAME}" > .properties
    echo "artifact=${ARTIFACT}" >> .properties
    echo "version=${VERSION}" >> .properties
    echo "authentication-required=false" >> .properties
    echo "builtime="`stat -c %y ${SOURCE}` >> .properties
    ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "mkdir -p /home/${SSH_AS_USER_NAME}/${DESTINATION}"
    scp -o StrictHostKeyChecking=no -i ~/.ssh/${SSH_KEY_NAME} ${SOURCE} ${SSH_AS_USER_NAME}@${AS_IP}:${DESTINATION}/${FILENAME}
    scp -o StrictHostKeyChecking=no -i ~/.ssh/${SSH_KEY_NAME} .properties ${SSH_AS_USER_NAME}@${AS_IP}:${DESTINATION}/.properties

    rm .properties
}

uploadArtifact() {
    ARTIFACT=$1

    FILENAME=`ls ${ARTIFACT}/target | grep -G ${ARTIFACT}-.*-binary[.]zip`
    VERSION=`ls ${ARTIFACT}/target | grep -G ${ARTIFACT}-.*[.]jar | grep -vE 'sources|original' | sed 's/'${ARTIFACT}'-//' | sed 's/.jar//'`
    SOURCE=${ARTIFACT}/target/${FILENAME}

    doUpload
}

uploadInstallScript() {
    ARTIFACT=install-script
    FILENAME=install-script.sh
    VERSION=`ls installation-manager/target | grep -G installation-manager-.*[.]jar | grep -vE 'sources|original' | sed 's/'installation-manager'-//' | sed 's/.jar//'`
    SOURCE=installation-manager/bin/${FILENAME}

    doUpload
}

uploadArtifact installation-manager
uploadArtifact installation-manager-cli
uploadInstallScript

