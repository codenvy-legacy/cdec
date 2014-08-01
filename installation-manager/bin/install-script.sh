#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/download/public/install-script.sh)

APP_DIR=$HOME/installation-manager

# TODO
# download install-manager
# add as service
# create user
#echo "launching puppet service..."
#sudo service puppet start

# download & unpack Installation Manager
#downloadUrl="https://codenvy.com/update/repository/download/public/installation-manager"
#filename=$(curl -sI  ${downloadUrl} | grep -o -E 'filename=.*$' | sed -e 's/filename=//')
#curl -o ${filename} -L ${downloadUrl}
#unzip ${filename} -d ${APP_DIR}

# create codenvy user and group
if [ `grep -c "^codenvy" /etc/group` == 0 ]; then
    sudo addgroup --quiet --gid 5001 codenvy
    echo "Group 'codenvy' has just been created"
fi

if [ `grep -c "^codenvy:" /etc/passwd` == 0 ]; then
    sudo adduser --quiet --home /home/codenvy --shel /bin/bash --uid 5001 --gid 5001 --disabled-password --gecos "" codenvy
    sudo passwd -q -d -l codenvy
    echo "User 'codenvy' has just been created"
fi

# register service
#sudo chkconfig puppet on
# cat /etc/*-release