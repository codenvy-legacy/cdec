#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/public/download/install-script)

CODENVY_USER=codenvy
CODENVY_HOME=/home/${CODENVY_USER}
APP_DIR=${CODENVY_HOME}/installation-manager
SCRIPT_NAME=installation-manager
SERVICE_NAME=codenvy-${SCRIPT_NAME}

createCodenvyUserAndGroupDebian() {
    if [ `grep -c "^${CODENVY_USER}" /etc/group` == 0 ]; then
        sudo addgroup --quiet --gid 5001 ${CODENVY_USER}
    fi
    
    if [ `grep -c "^${CODENVY_USER}:" /etc/passwd` == 0 ]; then
        sudo adduser --quiet --home ${CODENVY_HOME} --shell /bin/bash --uid 5001 --gid 5001 --disabled-password --gecos "" ${CODENVY_USER}
        sudo passwd -q -d -l ${CODENVY_USER}
    fi
}

createCodenvyUserAndGroupRedhat() {
    if [ `grep -c "^${CODENVY_USER}" /etc/group` == 0 ]; then
        sudo groupadd -g5001 ${CODENVY_USER}
    fi

    if [ `grep -c "^${CODENVY_USER}:" /etc/passwd` == 0 ]; then
        sudo useradd --home ${CODENVY_HOME} --shell /bin/bash --uid 5001 --gid 5001 ${CODENVY_USER}
        sudo passwd -l ${CODENVY_USER}
    fi
}

registerIMServiceDebian() {
    # http://askubuntu.com/questions/99232/how-to-make-a-jar-file-run-on-startup-and-when-you-log-out
    echo "> Register service"
    sudo update-rc.d ${SERVICE_NAME} defaults &>/dev/null
}

installJava() {
    command -v java >/dev/null 2>&1 || {     # check if requered program had already installed earlier
        echo "> Installation manager requires java to be installed "
        read -p "> Press any key to start downloading java" -n1 -s
        wget --no-cookies --no-check-certificate --header 'Cookie: oraclelicense=accept-securebackup-cookie' 'http://download.oracle.com/otn-pub/java/jdk/7u17-b02/jdk-7u17-linux-x64.tar.gz' --output-document=jdk.tar.gz

        echo "> Unpacking JDK binaries to /usr/local "
        sudo tar -xf jdk.tar.gz -C /usr/local

        sudo su - ${CODENVY_USER} -c "echo \"export JAVA_HOME=/usr/local/jdk1.7.0_17\" >> ~/.bashrc"
        sudo su - ${CODENVY_USER} -c "echo \"export PATH=$PATH:/usr/local/jdk1.7.0_17/bin\" >> ~/.bashrc"

        echo "export JAVA_HOME=/usr/local/jdk1.7.0_17" >> ~/.bashrc
        echo "export PATH=$PATH:/usr/local/jdk1.7.0_17/bin" >> ~/.bashrc
#        source ~/.bashrc

        rm jdk.tar.gz
        echo "> Java has been installed"
    }
}

registerIMServiceRedhat() {
    # http://www.abhigupta.com/2010/06/how-to-auto-start-services-on-boot-in-redhat-redhat/
    echo "> Registering Codenvy Installation Manage Service"
    sudo chkconfig --add ${SERVICE_NAME} &>/dev/null
    sudo chkconfig ${SERVICE_NAME} on &>/dev/null
}

installIM() {
    echo "> Downloading Installation Manager "

    DOWNLOAD_URL="https://codenvy.com/update/repository/public/download/installation-manager"

    filename=$(curl -sI  ${DOWNLOAD_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')
    curl -o ${filename} -L ${DOWNLOAD_URL}

    # removes existed files and creates new directory
    sudo rm ${APP_DIR} -rf
    sudo mkdir ${APP_DIR}

    # untar archive
    echo "> Unpacking binaries"
    sudo tar -xf ${filename} -C /tmp
    sudo tar -xf /tmp/${filename} -C ${APP_DIR}

    # move installation-manager script into /etc/init.d
    sudo mv ${APP_DIR}/${SCRIPT_NAME} /etc/init.d/${SERVICE_NAME}

    # make it possible to write files into the APP_DIR
    sudo chmod 757 ${APP_DIR}

    # make the user ${CODENVY_USER} an owner all files into the APP_DIR
    sudo chown -R ${CODENVY_USER}:${CODENVY_USER} ${APP_DIR}
}

installIMCLI() {
    DOWNLOAD_URL="https://codenvy.com/update/repository/public/download/installation-manager-cli"

    filename=$(curl -sI  ${DOWNLOAD_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')
    curl -o ${filename} -L ${DOWNLOAD_URL}

    # removes existed files and creates new directory
    rm ${HOME}/im-cli -rf &>/dev/null
    mkdir ${HOME}/im-cli

    # untar archive
    tar -xf ${filename} -C ${HOME}/im-cli

    cliinstalled=${HOME}/im-cli
    sudo su -c "echo ${cliinstalled} > ${CODENVY_HOME}/im-cli-instaled"
    sudo su -c "chown ${CODENVY_USER}:${CODENVY_USER} ${CODENVY_HOME}/im-cli-instaled"
}

launchingIMService() {
    echo "> Launching Codenvy Installation Manage Service"
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
    echo "> Operation system isn't supported."
    exit
fi

echo "> System is run on ${os} based distributive."

echo "> Creating user & group codenvy"
createCodenvyUserAndGroup${os}

installJava
installIM
installIMCLI

registerIMService${os}
launchingIMService


