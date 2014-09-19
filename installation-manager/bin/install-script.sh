#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/public/download/install-script)

USER=codenvy
CODENVY_HOME=/home/${USER}
APP_DIR=$CODENVY_HOME/installation-manager
SCRIPT_NAME=installation-manager
SERVICE_NAME=codenvy-${SCRIPT_NAME}

createCodenvyUserAndGroupDebian() {
    if [ `grep -c "^${USER}" /etc/group` == 0 ]; then
        sudo addgroup --quiet --gid 5001 ${USER}
    fi
    
    if [ `grep -c "^${USER}:" /etc/passwd` == 0 ]; then
        sudo adduser --quiet --home ${CODENVY_HOME} --shell /bin/bash --uid 5001 --gid 5001 --disabled-password --gecos "" ${USER}
        sudo passwd -q -d -l ${USER}
    fi
}

createCodenvyUserAndGroupRedhat() {
    if [ `grep -c "^${USER}" /etc/group` == 0 ]; then
        sudo groupadd -g5001 ${USER}
    fi

    if [ `grep -c "^${USER}:" /etc/passwd` == 0 ]; then
        sudo useradd --home ${CODENVY_HOME} --shell /bin/bash --uid 5001 --gid 5001 ${USER}
        sudo passwd -l ${USER}
    fi
}

registerServiceDebian() {
    # http://askubuntu.com/questions/99232/how-to-make-a-jar-file-run-on-startup-and-when-you-log-out
    echo "Register service..."
    sudo update-rc.d ${SERVICE_NAME} defaults &>/dev/null
}

installRequiredComponentsDebian() {
    echo "Check required components..."

    # install java
    command -v java >/dev/null 2>&1 || {     # check if requered program had already installed earlier
        echo "Installation manager requires java but it's not installed! " >&2
        read -p "Press any key to start installing java... " -n1 -s
        sudo apt-get install openjdk-7-jdk
    }
}

registerServiceRedhat() {
    # http://www.abhigupta.com/2010/06/how-to-auto-start-services-on-boot-in-redhat-redhat/
    echo "Register service..."
    sudo chkconfig --add ${SERVICE_NAME} &>/dev/null
    sudo chkconfig ${SERVICE_NAME} on &>/dev/null
}

installRequiredComponentsRedhat() {
    echo "Check required components..."

    # install java
    command -v java >/dev/null 2>&1 || {
        echo "Installing installation manager requires java but it's not installed! " >&2
        read -p "Press any key to start installing java... " -n1 -s
        sudo yum install java-1.7.0-openjdk
    }
}

installIM() {
    DOWNLOAD_URL="https://codenvy.com/update/repository/public/download/installation-manager"

    filename=$(curl -sI  ${DOWNLOAD_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')
    curl -o ${filename} -L ${DOWNLOAD_URL}

    # removes existed files and creates new directory
    sudo rm ${APP_DIR} -rf &>/dev/null
    sudo mkdir ${APP_DIR}

    # untar archive
    sudo tar -xvf ${filename} -C ${APP_DIR}

    # copy installation-manager script into /etc/init.d
    sudo cp ${APP_DIR}/${SCRIPT_NAME} /etc/init.d/${SERVICE_NAME}

    # make it possible to write files into the APP_DIR
    sudo chmod 757 ${APP_DIR}

    # make the user ${USER} an owner all files into the APP_DIR
    sudo chown -R ${USER}:${USER} ${APP_DIR}
}

installIMCLI() {
    DOWNLOAD_URL="https://codenvy.com/update/repository/public/download/installation-manager-cli"

    filename=$(curl -sI  ${DOWNLOAD_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')
    curl -o ${filename} -L ${DOWNLOAD_URL}

    # removes existed files and creates new directory
    rm ${HOME}/im-cli -rf &>/dev/null
    mkdir ${HOME}/im-cli

    # untar archive
    tar -xvf ${filename} -C ${HOME}/im-cli
}

launchingService() {
    # try to stop exists installation-manager
    sudo /etc/init.d/${SERVICE_NAME} stop
    sudo kill -9 $(ps aux | grep [i]nstallation-manager | cut -d" " -f4) &>/dev/null  # kill manager if stop isn't work

    # launch new installation-manager
    sudo /etc/init.d/${SERVICE_NAME} start
}

if [ -f /etc/debian_version ]; then
    os="Debian"
elif [ -f /etc/redhat-release ]; then
    os="Redhat"
else
    echo "Operation system isn't supported."
    exit
fi

echo "System is running on ${os} based distributive."

installRequiredComponents${os}
createCodenvyUserAndGroup${os}

echo "Installation: Installation Manager ..."
installIM

echo "Installation: Installation Manager CLI ..."
installIMCLI

registerService${os}
launchingService


