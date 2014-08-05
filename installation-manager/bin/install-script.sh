#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/download/public/install-script.sh)

USER=codenvy
HOME=/home/${USER}
APP_DIR=$HOME/installation-manager
SCRIPT_NAME=installation-manager
SERVICE_NAME=codenvy-${SCRIPT_NAME}

DOWNLOAD_URL="https://codenvy.com/update/repository/public/download/installation-manager"

create_codenvy_user_and_group_under_debian() {
    if [ `grep -c "^${USER}" /etc/group` == 0 ]; then
        sudo addgroup --quiet --gid 5001 ${USER}
        echo "Group '${USER}' has just been created"
    fi
    
    if [ `grep -c "^${USER}:" /etc/passwd` == 0 ]; then
        sudo adduser --quiet --home ${HOME} --shell /bin/bash --uid 5001 --gid 5001 --disabled-password --gecos "" ${USER}
        sudo passwd -q -d -l ${USER}
        echo "User '${USER}' has just been created"
    fi
}

register_service_under_debian() {
    # in Ubuntu OS http://askubuntu.com/questions/99232/how-to-make-a-jar-file-run-on-startup-and-when-you-log-out
    echo "Register service..."
    sudo update-rc.d ${SERVICE_NAME} defaults;
}

install_required_components_under_debian() {
   echo "Install required components..."
   sudo apt-get install openjdk-7-jdk
   sudo apt-get install unzip
}

create_codenvy_user_and_group_under_redhat() {
    if [ `grep -c "^${USER}" /etc/group` == 0 ]; then
        sudo groupadd -g5001 ${USER}
        echo "Group '${USER}' has just been created"
    fi

    if [ `grep -c "^${USER}:" /etc/passwd` == 0 ]; then
        sudo useradd --home ${HOME} --shell /bin/bash --uid 5001 --gid 5001 ${USER}
        sudo passwd -l ${USER}
        echo "User '${USER}' has just been created"
    fi
}

register_service_under_redhat() {
    # http://www.abhigupta.com/2010/06/how-to-auto-start-services-on-boot-in-redhat-redhat/
    echo "Register service..."
    sudo chkconfig --add ${SERVICE_NAME}
    sudo chkconfig ${SERVICE_NAME} on
}

install_required_components_under_redhat() {
    echo "Install required components..."
    sudo yum install java-1.7.0-openjdk
    sudo yum install unzip
}

download_and_unpack_instalation_manager() {
    echo "Download installation manager..."
    filename=$(curl -sI  ${DOWNLOAD_URL} | grep -o -E 'filename=.*$' | sed -e 's/filename=//')
    curl -o ${filename} -L ${DOWNLOAD_URL}

    echo "Unpack installation manager..."
    sudo unzip ${filename} -d ${APP_DIR}

    # copy installation-manager script into /etc/init.d
    sudo cp ${APP_DIR}/${SCRIPT_NAME} /etc/init.d/${SERVICE_NAME}

    # make it possible to write files into the APP_DIR
    sudo chmod 757 ${APP_DIR}
}

# Debian based linux distributives
if [ -f /etc/debian_version ]; then
    echo "System runs on Debian based distributive."
    install_required_components_under_debian

    create_codenvy_user_and_group_under_debian
     
    download_and_unpack_instalation_manager
    
    register_service_under_debian

    # launching service
    sudo /etc/init.d/${SERVICE_NAME} start

# RedHat based linux distributives
elif [ -f /etc/redhat-release ]; then
    echo "System runs on Red Hat based distributive."

    install_required_components_under_redhat

    create_codenvy_user_and_group_under_redhat
    
    download_and_unpack_instalation_manager
    
    register_service_under_redhat
    
    # launching service
    sudo /etc/init.d/${SERVICE_NAME} start

else
    echo "Operation system isn't supported."
fi
