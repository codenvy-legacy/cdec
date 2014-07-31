#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/download/public/install-script.sh)

# TODO
# download install-manager
# add as service
# create user

downloadUrl="https://codenvy.com/update/repository/download/public/installation-manager"
filename=$(curl -sI  ${downloadUrl} | grep -o -E 'filename=.*$' | sed -e 's/filename=//')
curl -o ${filename} -L ${downloadUrl}

#echo "launching puppet service..."
#sudo chkconfig puppet on
#sudo service puppet start
