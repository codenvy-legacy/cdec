#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/public/download/install-script)

USER=codenvy
HOME=/home/${USER}
APP_DIR=$HOME/installation-manager
SCRIPT_NAME=installation-manager
SERVICE_NAME=codenvy-${SCRIPT_NAME}

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
    # http://askubuntu.com/questions/99232/how-to-make-a-jar-file-run-on-startup-and-when-you-log-out
    echo "Register service..."
    sudo update-rc.d ${SERVICE_NAME} defaults &>-
}

install_required_components_under_debian() {
    echo "Check required components..."

    # install java
    command -v java >/dev/null 2>&1 || {     # check if requered program had already installed earlier
        echo "Installation manager requires java but it's not installed! " >&2
        read -p "Press any key to start installing java... " -n1 -s
        sudo apt-get install openjdk-7-jdk
    }
    
    # install unzip
    command -v unzip >/dev/null 2>&1 || {      # check if requered program had already installed earlier
        echo "Installing installation manager requires unzip but it's not installed! " >&2
        read -p "Press any key to start installing unzip... " -n1 -s
        sudo apt-get install unzip
    }
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
    sudo chkconfig --add ${SERVICE_NAME} &>-
    sudo chkconfig ${SERVICE_NAME} on &>-
}

install_required_components_under_redhat() {
    echo "Check required components..."

    # install java
    command -v java >/dev/null 2>&1 || {    # check if requered program had already installed earlier
        echo "Installation manager requires java but it's not installed! " >&2
        read -p "Press any key to start installing java... " -n1 -s
        sudo yum install java-1.7.0-openjdk
    } 
    
    # install unzip
    command -v unzip >/dev/null 2>&1 || {   # check if requered program had already installed earlier
        echo "Installing installation manager requires unzip but it's not installed! " >&2
        read -p "Press any key to start installing unzip... " -n1 -s
        sudo yum install unzip
    }
}

installIM() {
    DOWNLOAD_URL="https://codenvy.com/update/repository/public/download/installation-manager"

    echo "Download installation manager..."
    filename=$(curl -sI  ${DOWNLOAD_URL} | grep -o -E 'filename=(.*)[.]zip' | sed -e 's/filename=//')
    curl -o ${filename} -L ${DOWNLOAD_URL}

    echo "Unpack installation manager..."
    sudo rm ${APP_DIR} -r &>-                 # remove exists files
    sudo unzip ${filename} -d ${APP_DIR} &>-  # unzip new package

    # copy installation-manager script into /etc/init.d
    sudo cp ${APP_DIR}/${SCRIPT_NAME} /etc/init.d/${SERVICE_NAME}

    # make it possible to write files into the APP_DIR
    sudo chmod 757 ${APP_DIR}
}

installCLI() {
    DOWNLOAD_URL="https://codenvy.com/update/repository/public/download/installation-manager-cli"

    echo "Download installation manager CLI..."
    filename=$(curl -sI  ${DOWNLOAD_URL} | grep -o -E 'filename=(.*)[.]zip' | sed -e 's/filename=//')
    curl -o ${filename} -L ${DOWNLOAD_URL}

    echo "Unpack installation manager CLI..."
    rm ${HOME}/im-cli -r &>-                 # remove exists files
    unzip ${filename} -d ${HOME}/im-cli &>-  # unzip new package
}

launching_service() {
    # try to stop exists installation-manager
    sudo /etc/init.d/${SERVICE_NAME} stop
    sudo kill -9 $(ps aux | grep [i]nstallation-manager | cut -d" " -f4) &>-  # kill manager if stop isn't work

    # launch new installation-manager
    sudo /etc/init.d/${SERVICE_NAME} start
}

# Debian based linux distributives
if [ -f /etc/debian_version ]; then
    echo "System runs on Debian based distributive."
    install_required_components_under_debian
    create_codenvy_user_and_group_under_debian
    installIM
    installCLI
    register_service_under_debian
    launching_service

# RedHat based linux distributives
elif [ -f /etc/redhat-release ]; then
    echo "System runs on Red Hat based distributive."
    install_required_components_under_redhat
    create_codenvy_user_and_group_under_redhat    
    installIM
    installCLI
    register_service_under_redhat
    launching_service

else
    echo "Operation system isn't supported."
fi
