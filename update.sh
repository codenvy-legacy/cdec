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
filename=`ls cdec-packaging-tomcat-update-server/target | grep update-server-tomcat-pkg`
if [ -z "$1" ] || [ "$1" == "prod" ]; then
    SSH_KEY_NAME=cl-server-prod-20130219
#    SSH_AS_USER_NAME=logreader
#    AS_IP=syslog.codenvycorp.com
    echo "============[ Production will be updated ]=============="
elif [ "$1" == "stg" ]; then
    SSH_KEY_NAME=as1-cldide_cl-server.skey
    SSH_AS_USER_NAME=codenvy
    AS_IP=syslog.codenvy-stg.com
    echo "============[ Staging will be updated ]=============="
else
    exit
fi
home=/home/${SSH_AS_USER_NAME}/update-server-tomcat

deleteFileIfExists() {
    if [ -f $1 ]; then
        echo $1
        rm -rf $1
    fi
}

    echo "==== Step [1/7] =======================> [Uploading a new Tomcat]"
    scp -o StrictHostKeyChecking=no -i ~/.ssh/${SSH_KEY_NAME} cdec-packaging-tomcat-update-server/target/${filename} ${SSH_AS_USER_NAME}@${AS_IP}:${filename}
    echo "==== Step [2/7] =======================> [Stoping Tomcat]"
    ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "cd ${home}/bin/;if [ -f catalina.sh ]; then ./catalina.sh stop; fi"
    echo "==== Step [3/7] =======================> [Server is stopped]"
    echo "==== Step [4/7] =======================> [Cleaning up]"
    ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "rm -rf ${home}"
    echo "==== Step [5/7] =======================> [Unpacking resources]"
    ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "unzip ${filename} -d update-server-tomcat"

    if [ "$1" == "stg" ]; then
        ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "sed -i 's/8080/9080/g' ${home}/conf/server.xml"
        ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "sed -i 's/8005/9005/g' ${home}/conf/server.xml"
        ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "sed -i 's/8443/9443/g' ${home}/conf/server.xml"
        ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "sed -i 's/32001/33001/g' ${home}/conf/server.xml"
        ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "sed -i 's/32101/33101/g' ${home}/conf/server.xml"
    fi

    echo "==== Step [6/7] =======================> [Starting up on ${AS_IP}]"
    ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "cd ${home}/bin;./catalina.sh start"

    AS_STATE='Starting'
    testfile=/tmp/catalina.log
    while [ "${AS_STATE}" != "Started" ]; do

        deleteFileIfExists ${testfile}
        scp -o StrictHostKeyChecking=no -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP}:${home}/logs/catalina.out ${testfile}

        if grep -Fq "Server startup in" ${testfile}; then
            echo "==== Step [7/7] ======================> [update-server is started]"
            AS_STATE=Started
        fi
            sleep 5
    done
    echo ""
    echo ""
    echo "============================================================================"
    echo "====================== UPDATE SERVICE INSTALLED ============================"
    echo "============================================================================"
