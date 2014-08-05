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
    SERVER_IP=https://codenvy.com
    echo "Uploading on production"
elif [ "$1" == "stg" ]; then
    SERVER_IP=https://codenvy-stg.com
    echo "Uploading on staging"
else
    echo "Unknown server destination"
    exit 1
fi

if [ "$2" == "" ] || [ "$3" == "" ]; then
    echo "User or password didn't set"
    exit 1
fi

USERNAME=$2
PASSWORD=$3

# recieve token
echo "Log in using $SERVER_IP"
curl -X POST -H "Content-Type: application/json" -d '{"username":"'${USERNAME}'","password":"'${PASSWORD}'","realm":"sysldap"}' --insecure ${SERVER_IP}/api/auth/login > response
TOKEN=$(cat response | sed 's/"}//' | sed 's/{"value":"//' )

response=`cat response`
if [ "${response}" == "Invalid user name or password" ]; then
    echo ${response}
    rm response
    exit 1
fi

# uploading
FILENAME=`ls installation-manager-cli/target | grep -G installation-manager-cli-.*-binary[.]zip`
VERSION=`ls installation-manager-cli/target | grep -G installation-manager-cli-.*[.]jar | grep -vE 'sources|original' | sed 's/installation-manager-cli-//' | sed 's/.jar//'`
echo "Uploading $FILENAME started"
curl -X POST -F "file=@installation-manager-cli/target/"${FILENAME} --insecure ${SERVER_IP}/update/repository/upload/installation-manager-cli/${VERSION}?token=${TOKEN}'&'authentication_required=false > response

response=`cat response`
if [ "${response}" != "" ]; then
    echo ${response}
    rm response
    exit 1
fi

echo "File uploaded succesfully"
rm response
exit 0
