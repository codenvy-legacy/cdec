#!/bin/bash

# bash <(curl -L -s https://start.codenvy.com/install-single)
#
# allowed options:
# --multi
# --silent
# --version=<VERSION TO INSTALL>
# --hostname=<CODENVY HOSTNAME>
# --systemAdminName=<SYSTEM ADMIN NAME>
# --systemAdminPassword=<SYSTEM ADMIN PASSWORD>

trap cleanUp EXIT

unset HOST_NAME
unset SYSTEM_ADMIN_NAME
unset SYSTEM_ADMIN_PASSWORD
unset PROGRESS_PID

JDK_URL=http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jdk-8u45-linux-x64.tar.gz
JRE_URL=http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jre-8u45-linux-x64.tar.gz

PUPPET_MASTER_PORTS=("tcp:8140");
SITE_PORTS=("tcp:80" "tcp:443" "tcp:10050" "tcp:32001" "tcp:32101");
API_PORTS=("tcp:8080" "tcp:8180" "tcp:10050" "tcp:32001" "tcp:32101" "tcp:32201" "tcp:32301");
DATA_PORTS=("tcp:389" "tcp:5432" "tcp:10050" "tcp:27017" "tcp:28017");
DATASOURCE_PORTS=("tcp:8080" "tcp:10050" "tcp:32001" "tcp:32101");
RUNNER_PORTS=("tcp:80" "tcp:8080" "tcp:10050" "tcp:32001" "tcp:32101");
BUILDER_PORTS=("tcp:8080" "tcp:10050" "tcp:32001" "tcp:32101");
ANALYTICS_PORTS=("tcp:7777" "tcp:8080" "udp:5140" "tcp:9763" "tcp:10050" "tcp:32001" "tcp:32101");

function cleanUp() {
    killTimer

    printLn
    printLn
}

validateExitCode() {
    EXIT_CODE=$1
    if [[ ! -z ${EXIT_CODE} ]] && [[ ! ${EXIT_CODE} == "0" ]]; then
        pauseTimer
        printLn
        printLn "Unexpected error occurred. See install.log for more details"
        exit ${EXIT_CODE}
    fi
}

setRunOptions() {
    START_TIME=`date +%s`
    DIR="${HOME}/codenvy-im"
    ARTIFACT="codenvy"
    CODENVY_TYPE="single"
    SILENT=false
    VERSION=`curl -s https://codenvy.com/update/repository/properties/${ARTIFACT} | sed 's/.*"version":"\([^"]*\)".*/\1/'`
    for var in "$@"; do
        if [[ "$var" == "--multi" ]]; then
            CODENVY_TYPE="multi"
        elif [[ "$var" == "--silent" ]]; then
            SILENT=true
        elif [[ "$var" =~ --version=.* ]]; then
            VERSION=`echo "$var" | sed -e "s/--version=//g"`
        elif [[ "$var" =~ --hostname=.* ]]; then
            HOST_NAME=`echo "$var" | sed -e "s/--hostname=//g"`
        elif [[ "$var" =~ --systemAdminName=.* ]]; then
            SYSTEM_ADMIN_NAME=`echo "$var" | sed -e "s/--systemAdminName=//g"`
        elif [[ "$var" =~ --systemAdminPassword=.* ]]; then
            SYSTEM_ADMIN_PASSWORD=`echo "$var" | sed -e "s/--systemAdminPassword=//g"`
        fi
    done
    CONFIG="codenvy-${CODENVY_TYPE}-server.properties"

    if [[ ${CODENVY_TYPE} == "single" ]] && [[ ! -z ${HOST_NAME} ]] && [[ ! -z ${SYSTEM_ADMIN_PASSWORD} ]] && [[ ! -z ${SYSTEM_ADMIN_NAME} ]]; then
        SILENT=true
    fi
}

downloadConfig() {
    url="https://codenvy.com/update/repository/public/download/codenvy-${CODENVY_TYPE}-server-properties/${VERSION}"

    # check url to config on http error
    http_code=$(curl --silent --write-out '%{http_code}' --output /dev/null ${url})
    if [[ ! ${http_code} -eq 200 ]]; then    # if response code != "200 OK"
        # print response from update server
        printLn $(curl --silent ${url})
        exit 1
    fi

    # load config into the ${CONFIG} file
    curl --silent --output ${CONFIG} ${url}
}

validateOS() {
    if [ -f /etc/redhat-release ]; then
        osToDisplay="Red Hat"
    else
        printLn  "Operation system isn't supported."
        exit 1
    fi

    OS=`cat /etc/redhat-release`
    osVersion=`${osToDisplay} | sed 's/.* \([0-9.]*\) .*/\1/' | cut -f1 -d '.'`

    if [ "${VERSION}" == "3.1.0" ] && [ "${osVersion}" != "6" ]; then
        printLn "Codenvy 3.1.0 can be installed onto CentOS 6.x only"
        exit 1
    fi

    if [ "${CODENVY_TYPE}" == "multi" ] && [ "${osVersion}" != "7" ]; then
        printLn "Codenvy multi-node can be installed onto CentOS 7.x only"
        exit 1
    fi
}

# $1 - command name
installPackageIfNeed() {
    command -v $1 >/dev/null 2>&1 || { # check if requered command had been already installed earlier
        sudo yum install $1 -y -q
    }
}

preconfigureSystem() {
    sudo yum clean all &> /dev/null
    installPackageIfNeed curl
    installPackageIfNeed net-tools

    if [[ ! -f ${CONFIG} ]]; then
        downloadConfig
    fi
}

installJava() {
    wget -q --no-cookies --no-check-certificate --header "Cookie: oraclelicense=accept-securebackup-cookie" "${JRE_URL}" --output-document=jre.tar.gz

    tar -xf jre.tar.gz -C ${DIR}
    mv ${DIR}/jre1.8.0_45 ${DIR}/jre

    rm jre.tar.gz
}

installIm() {
    IM_URL="https://codenvy.com/update/repository/public/download/installation-manager-cli"
    IM_FILE=$(curl -sI  ${IM_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')

    curl -s -o ${IM_FILE} -L ${IM_URL}

    mkdir ${DIR}/codenvy-cli
    tar -xf ${IM_FILE} -C ${DIR}/codenvy-cli

    sed -i "2iJAVA_HOME=${HOME}/codenvy-im/jre" ${DIR}/codenvy-cli/bin/codenvy
}

clearLine() {
    echo -en "\033[2K"                  # clear line
}

cursorUp() {
    echo -en "\e[1A"
}

printPrompt() {
    clearLine
    echo -en "\e[94m[CODENVY]\e[0m "    # with blue color
}

printRed() {
    echo -en "\e[91m$1\e[0m" # with red color
}

printGreen() {
    echo -en "\e[32m$1\e[0m" # with green color
}

print() {
    printPrompt; echo -n "$@"
}

printLn() {
    printPrompt; echo "$@"
}

askProperty() {
    read VALUE
    echo ${VALUE}
}

insertProperty() {
    sed -i s/$1=.*/$1=$2/g ${CONFIG}
}

validateHostname() {
    DNS=$1

    OUTPUT=$(ping -c 1 ${DNS} &> /dev/null && echo success || echo fail)

    echo ${OUTPUT}
}

askHostnameAndInsertProperty() {
    PROMPT=$1
    VARIABLE=$2

    FIRST_ATTEMPT=true

    while :
    do
        if [[ ${FIRST_ATTEMPT} == false ]]; then
            cursorUp
            cursorUp
            clearLine
        else
            FIRST_ATTEMPT=false
        fi

        print "$(printf "%-35s" "${PROMPT}:") "

        read VALUE

        OUTPUT=$(validateHostname ${VALUE})
        if [ "${OUTPUT}" == "success" ]; then
           break
        else
            printLn "$(printRed "ERROR"): The hostname '${VALUE}' isn't availabe or wrong. Please try again..."
        fi
    done

    insertProperty "${VARIABLE}" ${VALUE}
}

executeIMCommand() {
    ${DIR}/codenvy-cli/bin/codenvy $@
}

pressAnyKeyToContinueAndClearConsole() {
    if [[ ${SILENT} == false ]]; then
        printLn  "Press any key to continue"
        read -n1 -s
        clear
    fi
}

pressAnyKeyToContinue() {
    if [[ ${SILENT} == false ]]; then
        printLn  "Press any key to continue"
        read -n1 -s
    fi
}

pressYKeyToContinue() {
    if [[ ${SILENT} == false ]]; then
        if [[ ! -z $1 ]]; then
            print $@
        else
            print "Continue installation"
        fi
        echo -n " [y/N]: "

        read ANSWER
        if [[ ! "${ANSWER}" == "y" ]]; then
            exit 1
        fi
    fi
}

doCheckPortRemote() {
    PROTOCOL=$1
    PORT=$2
    HOST=$3
    OUTPUT=$(ssh -o LogLevel=quiet -o StrictHostKeyChecking=no -t $HOST "netstat -ano | egrep LISTEN | egrep ${PROTOCOL} | egrep ':${PORT}\s'")
    echo ${OUTPUT}
}

doCheckPortLocal() {
    PROTOCOL=$1
    PORT=$2
    OUTPUT=$(netstat -ano | egrep LISTEN | egrep ${PROTOCOL} | egrep ":${PORT}\s")
    echo ${OUTPUT}
}

validatePortLocal() {
    PROTOCOL=$1
    PORT=$2
    OUTPUT=$(doCheckPortLocal ${PROTOCOL} ${PORT})

    if [ "${OUTPUT}" != "" ]; then
        printLn "$(printRed "ERROR"): The port ${PROTOCOL}:${PORT} is busy."
        printLn "$(printRed "ERROR"): The installation can't be proceeded."
        exit 1
    fi
}

validatePortRemote() {
    PROTOCOL=$1
    PORT=$2
    HOST=$3
    OUTPUT=$(doCheckPortRemote ${PROTOCOL} ${PORT} ${HOST})

    if [ "${OUTPUT}" != "" ]; then
        printLn "$(printRed "ERROR"): The port ${PROTOCOL}:${PORT} on host ${HOST} is busy."
        printLn "$(printRed "ERROR"): The installation can't be proceeded."
        exit 1
    fi
}

doGetHostsVariables() {
    HOST_NAME=$(grep host_url\\s*=\\s*.* ${CONFIG} | sed 's/host_url\s*=\s*\(.*\)/\1/')
    PUPPET_MASTER_HOST_NAME=`grep puppet_master_host_name=.* ${CONFIG} | cut -f2 -d '='`
    DATA_HOST_NAME=`grep data_host_name=.* ${CONFIG} | cut -f2 -d '='`
    API_HOST_NAME=`grep api_host_name=.* ${CONFIG} | cut -f2 -d '='`
    BUILDER_HOST_NAME=`grep builder_host_name=.* ${CONFIG} | cut -f2 -d '='`
    RUNNER_HOST_NAME=`grep runner_host_name=.* ${CONFIG} | cut -f2 -d '='`
    DATASOURCE_HOST_NAME=`grep datasource_host_name=.* ${CONFIG} | cut -f2 -d '='`
    ANALYTICS_HOST_NAME=`grep analytics_host_name=.* ${CONFIG} | cut -f2 -d '='`
    SITE_HOST_NAME=`grep site_host_name=.* ${CONFIG} | cut -f2 -d '='`
}

doCheckAvailablePorts_single() {
    for PORT in ${PUPPET_MASTER_PORTS[@]} ${SITE_PORTS[@]} ${API_PORTS[@]} ${DATA_PORTS[@]} ${DATASOURCE_PORTS[@]} ${RUNNER_PORTS[@]} ${BUILDER_PORTS[@]} ${ANALYTICS_PORTS[@]}; do
        PROTOCOL=`echo ${PORT}|awk -F':' '{print $1}'`;
        PORT_ONLY=`echo ${PORT}|awk -F':' '{print $2}'`;

        validatePortLocal "${PROTOCOL}" "${PORT_ONLY}"
    done
}

doCheckAvailablePorts_multi() {
    doGetHostsVariables

    for HOST in ${PUPPET_MASTER_HOST_NAME} ${DATA_HOST_NAME} ${API_HOST_NAME} ${BUILDER_HOST_NAME} ${DATASOURCE_HOST_NAME} ${ANALYTICS_HOST_NAME} ${SITE_HOST_NAME} ${RUNNER_HOST_NAME}; do
        if [[ ${HOST} == ${PUPPET_MASTER_HOST_NAME} ]]; then
            PORTS=${PUPPET_MASTER_PORTS[@]}
        elif [[ ${HOST} == ${DATA_HOST_NAME} ]]; then
            PORTS=${DATA_PORTS[@]}
        elif [[ ${HOST} == ${API_HOST_NAME} ]]; then
            PORTS=${API_PORTS[@]}
        elif [[ ${HOST} == ${BUILDER_HOST_NAME} ]]; then
            PORTS=${BUILDER_PORTS[@]}
        elif [[ ${HOST} == ${DATASOURCE_HOST_NAME} ]]; then
            PORTS=${DATASOURCE_PORTS[@]}
        elif [[ ${HOST} == ${ANALYTICS_HOST_NAME} ]]; then
            PORTS=${ANALYTICS_PORTS[@]}
        elif [[ ${HOST} == ${SITE_HOST_NAME} ]]; then
            PORTS=${SITE_PORTS[@]}
        elif [[ ${HOST} == ${RUNNER_HOST_NAME} ]]; then
            PORTS=${RUNNER_PORTS[@]}
        fi

        for PORT in ${PORTS[@]}; do
            PROTOCOL=`echo ${PORT}|awk -F':' '{print $1}'`;
            PORT_ONLY=`echo ${PORT}|awk -F':' '{print $2}'`;

            if [[ ${HOST} == ${PUPPET_MASTER_HOST_NAME} ]]; then
                validatePortLocal "${PROTOCOL}" "${PORT_ONLY}"
            else
                validatePortRemote "${PROTOCOL}" "${PORT_ONLY}" ${HOST}
            fi
        done
    done
}

printPreInstallInfo_single() {
    clear

    printLn "Welcome. This program installs Codenvy "${VERSION}
    printLn
    printLn "Sizing Guide:        http://docs.codenvy.com/onprem"
    printLn "Configuration File:  "${CONFIG}
    printLn
    printLn "Checking system pre-requisites..."
    printLn

    preconfigureSystem
    doCheckAvailableResourcesLocally 8000000 4 300000000

    printLn "Checking access to external dependencies..."
    printLn
    checkingAccessToExternalDependencies

    [ ! -z "${SYSTEM_ADMIN_NAME}" ] && insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
    [ ! -z "${SYSTEM_ADMIN_PASSWORD}" ] && insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
    [ ! -z "${HOST_NAME}" ] && insertProperty "host_url" ${HOST_NAME}

    doCheckAvailablePorts_single
    printLn
    printLn
    printLn
}

# parameter 1 - MIN_RAM_KB
# parameter 2 - MIN_CORES
# parameter 3 - MIN_DISK_SPACE_KB
doCheckAvailableResourcesLocally() {
    MIN_RAM_KB=$1
    MIN_CORES=$2
    MIN_DISK_SPACE_KB=$3

    osIssueFound=false
    osType=""
    osVersion=""
    osInfo=""

    case `uname` in
        Linux )
            # CentOS
            if [ -f /etc/redhat-release ] ; then
                osType="CentOS"
                osVersion=`cat /etc/redhat-release | sed 's/.* \([0-9.]*\) .*/\1/' | cut -f1 -d '.'`
                osInfo=`cat /etc/redhat-release | sed 's/Linux release //'`

            # SuSE
            elif [ -f /etc/SuSE-release ] ; then
                osInfo="SuSE"

            # debian
            elif [ -f /etc/debian_version ]; then
                osInfo=`cat /etc/issue.net`

            # other linux OS
            elif [ -f /etc/lsb-release ]; then
                osInfo=$(cat /etc/lsb-release | grep '^DISTRIB_ID' | awk -F=  '{ print $2 }')
            fi
            ;;

        * )
            osInfo=`uname`;
            ;;
    esac

    # check on OS CentOS 7
    if [[ ${osType} != "CentOS" || ${osVersion} != "7" ]]; then
        osIssueFound=true
    fi

    osInfoToDisplay=$(printf "%-30s" "${osInfo}")
    osStateToDisplay=$([ ${osIssueFound} == false ] && echo "$(printGreen "[OK]")" || echo "$(printRed "[NOT OK]")")
    printLn "DETECTED OS: ${osInfoToDisplay} ${osStateToDisplay}"


    resourceIssueFound=false

    availableRAM=`cat /proc/meminfo | grep MemTotal | awk '{print $2}'`
    availableRAMIssue=false

    availableDiskSpace=`sudo df ${HOME} | tail -1 | awk '{print $2}'`
    availableDiskSpaceIssue=false

    availableCores=`grep -c ^processor /proc/cpuinfo`
    availableCoresIssue=false

    if (( ${availableRAM} < ${MIN_RAM_KB} )); then
        resourceIssueFound=true
        availableRAMIssue=true
    fi

    if (( ${availableCores} < ${MIN_CORES})); then
        resourceIssueFound=true
        availableCoresIssue=true
    fi

    if (( ${availableDiskSpace} < ${MIN_DISK_SPACE_KB})); then
        resourceIssueFound=true
        availableDiskSpaceIssue=true
    fi

    minRAMToDisplay=$(printf "%-15s" "$(printf "%0.2f" "$( m=34; awk -v m=${MIN_RAM_KB} 'BEGIN { print m/1000/1000 }' )") GB")
    availableRAMToDisplay=`cat /proc/meminfo | grep MemTotal | awk '{tmp = $2/1000/1000; printf"%0.2f",tmp}'`
    availableRAMToDisplay=$(printf "%-11s" "${availableRAMToDisplay} GB")
    RAMStateToDisplay=$([ ${availableRAMIssue} == false ] && echo "$(printGreen "[OK]")" || echo "$(printRed "[NOT OK]")")

    minCoresToDisplay=$(printf "%-15s" "${MIN_CORES} cores")
    availableCoresToDisplay=$(printf "%-11s" "${availableCores} cores")
    coresStateToDisplay=$([ ${availableCoresIssue} == false ] && echo "$(printGreen "[OK]")" || echo "$(printRed "[NOT OK]")")

    minDiskSpaceToDisplay=$(printf "%-15s" "$(( ${MIN_DISK_SPACE_KB} /1000/1000 )) GB")
    availableDiskSpaceToDisplay=$(( availableDiskSpace /1000/1000 ))
    availableDiskSpaceToDisplay=$(printf "%-11s" "${availableDiskSpaceToDisplay} GB")
    diskStateToDisplay=$([ ${availableDiskSpaceIssue} == false ] && echo "$(printGreen "[OK]")" || echo "$(printRed "[NOT OK]")")

    printLn
    printLn "                RECOMMENDED     AVAILABLE"
    printLn "RAM             ${minRAMToDisplay} ${availableRAMToDisplay} ${RAMStateToDisplay}"
    printLn "CPU             ${minCoresToDisplay} ${availableCoresToDisplay} ${coresStateToDisplay}"
    printLn "Disk Space      ${minDiskSpaceToDisplay} ${availableDiskSpaceToDisplay} ${diskStateToDisplay}"
    printLn

    if [[ ${osIssueFound} == true || ${resourceIssueFound} == true ]]; then
        if [[ ${osIssueFound} == true ]]; then
            printLn "!!! The OS version or config do not match requirements. !!!"
            exit 1;
        fi

        if [[ ${resourceIssueFound} == true ]]; then
            printLn "!!! The resources available are lower than required.    !!!"
            printLn "!!! Troubleshooting: http://docs.codenvy.com/onprem     !!!"
        fi

        printLn

        if [[ ${SILENT} == false && ${resourceIssueFound} == true ]]; then
            pressYKeyToContinue "Proceed?"
            printLn
        fi
    fi
}

checkingAccessToExternalDependencies() {
    resourceIssueFound=false

    checkUrl https://install.codenvycorp.com || resourceIssueFound=true
    checkUrl http://archive.apache.org/dist/ant/binaries || resourceIssueFound=true
    checkUrl ${JDK_URL} "Cookie: oraclelicense=accept-securebackup-cookie" || resourceIssueFound=true
    checkUrl http://dl.fedoraproject.org/pub/epel/ || resourceIssueFound=true
    checkUrl https://storage.googleapis.com/appengine-sdks/ || resourceIssueFound=true
    checkUrl http://www.us.apache.org/dist/maven/ || resourceIssueFound=true
    checkUrl https://repo.mongodb.org/yum/redhat/ || resourceIssueFound=true
    checkUrl http://repo.mysql.com/ || resourceIssueFound=true
    checkUrl http://nginx.org/packages/centos/ || resourceIssueFound=true
    checkUrl http://yum.postgresql.org/ || resourceIssueFound=true
    checkUrl http://yum.puppetlabs.com/ || resourceIssueFound=true
    checkUrl http://repo.zabbix.com/zabbix/ || resourceIssueFound=true
    checkUrl http://mirror.centos.org/centos/ || resourceIssueFound=true

    printLn

    if [[ ${resourceIssueFound} == true ]]; then
        printLn "!!! Some repositories are not accessible. The installation will fail. !!!"
        printLn "!!! Consider setting up a proxy server.                               !!!"
        printLn "!!! See: http://docs.codenvy.com/onprem/installation-bootstrap/       !!!"
        printLn

        if [[ ${SILENT} == true ]]; then
            exit 1;
        fi

        pressYKeyToContinue "Proceed?"
        printLn
    fi
}

# parameter 1 - url
# parameter 2 - cookie
checkUrl() {
    checkFailed=0
    url=$1
    cookie=$2

    if [[ ${cookie} == "" ]]; then
        wget --quiet --spider ${url} || checkFailed=1
    else
        wget --quiet --spider --no-cookies --no-check-certificate --header "${cookie}" ${url} || checkFailed=1
    fi

    printLn "$(printf "%-79s" ${url})"$([ ${checkFailed} == 0 ] && echo "$(printGreen "[OK]")" || echo "$(printRed "[NOT OK]")")

    return ${checkFailed}
}

printPreInstallInfo_multi() {
    clear

    printLn "Welcome. This program installs Codenvy "${VERSION}
    printLn
    printLn "Sizing Guide:        http://docs.codenvy.com/onprem"
    printLn "Configuration File:  "${CONFIG}
    printLn
    printLn "Checking system pre-requisites..."
    printLn

    preconfigureSystem
    doCheckAvailableResourcesLocally 1000000 1 14000000

    [ ! -z "${SYSTEM_ADMIN_NAME}" ] && insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
    [ ! -z "${SYSTEM_ADMIN_PASSWORD}" ] && insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}

    if [[ ${SILENT} == true ]]; then
        [ ! -z "${HOST_NAME}" ] && insertProperty "host_url" ${HOST_NAME}

        doGetHostsVariables

        printLn "Hostname of Codenvy              : "${HOST_NAME}
        printLn "Hostname of Puppet master node   : "${PUPPET_MASTER_HOST_NAME}
        printLn "Hostname of data node            : "${DATA_HOST_NAME}
        printLn "Hostname of API node             : "${API_HOST_NAME}
        printLn "Hostname of builder node         : "${BUILDER_HOST_NAME}
        printLn "Hostname of runner node          : "${RUNNER_HOST_NAME}
        printLn "Hostname of datasource node      : "${DATASOURCE_HOST_NAME}
        printLn "Hostname of analytics node       : "${ANALYTICS_HOST_NAME}
        printLn "Hostname of site node            : "${SITE_HOST_NAME}
        printLn
    else
        printLn "Codenvy hostnames:       will prompt for entry"
        printLn

        askHostnameAndInsertProperty "Set hostname of Codenvy" "host_url"
        askHostnameAndInsertProperty "Set hostname of Puppet master node" "puppet_master_host_name"
        askHostnameAndInsertProperty "Set hostname of data node" "data_host_name"
        askHostnameAndInsertProperty "Set hostname of API node" "api_host_name"
        askHostnameAndInsertProperty "Set hostname of builder node" "builder_host_name"
        askHostnameAndInsertProperty "Set hostname of runner node" "runner_host_name"
        askHostnameAndInsertProperty "Set hostname of datasource node" "datasource_host_name"
        askHostnameAndInsertProperty "Set hostname of analytics node" "analytics_host_name"
        askHostnameAndInsertProperty "Set hostname of site node" "site_host_name"

        clearLine

        printLn
        pressYKeyToContinue "Proceed?"
        printLn
    fi

    printLn "Checking access to Codenvy nodes..."
    printLn
    doCheckAvailableResourcesOnNodes
    printLn
    doCheckAvailablePorts_multi

    printLn "Checking access to external dependencies..."
    printLn
    checkingAccessToExternalDependencies
    printLn
    printLn
}

doCheckAvailableResourcesOnNodes() {
    globalNodeIssueFound=false
    globalOsIssueFound=false

    doGetHostsVariables

    for HOST in ${PUPPET_MASTER_HOST_NAME} ${DATA_HOST_NAME} ${API_HOST_NAME} ${BUILDER_HOST_NAME} ${DATASOURCE_HOST_NAME} ${ANALYTICS_HOST_NAME} ${SITE_HOST_NAME} ${RUNNER_HOST_NAME}; do
        # check if host available
        OUTPUT=$(validateHostname ${HOST})
        if [ "${OUTPUT}" != "success" ]; then
            printLn "$(printRed "ERROR"): The hostname '${HOST}' isn't availabe or wrong."
            exit 1
        fi

        SSH_PREFIX="ssh -o LogLevel=quiet -o StrictHostKeyChecking=no -t ${HOST}"

        if [[ ${HOST} == ${RUNNER_HOST_NAME} ]]; then
            MIN_RAM_KB=1500000
            MIN_DISK_SPACE_KB=50000000
        else
            MIN_RAM_KB=1000000
            MIN_DISK_SPACE_KB=14000000
        fi

        osIssueFound=false

        osType=""
        osVersion=""
        osInfo=""

        case `${SSH_PREFIX} "uname" | sed 's/\r//'` in
            Linux )
                if [[ `${SSH_PREFIX} "if [[ -f /etc/redhat-release ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osType="CentOS";
                    osVersion=`${SSH_PREFIX} "cat /etc/redhat-release" | sed 's/.* \([0-9.]*\) .*/\1/' | cut -f1 -d '.'`
                    osInfo=`${SSH_PREFIX} "cat /etc/redhat-release" | sed 's/Linux release //' | sed 's/\r//'`

                # SuSE
                elif [[ `${SSH_PREFIX} "if [[ -f /etc/SuSE-release ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osInfo="SuSE"

                # debian
                elif [[ `${SSH_PREFIX} "if [[ -f /etc/debian_version ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osInfo=`${SSH_PREFIX} "cat /etc/issue.net" | sed 's/\r//'`

                # other linux OS
                elif [[ `${SSH_PREFIX} "if [[ -f /etc/lsb-release ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osInfo=`${SSH_PREFIX} "$(cat /etc/lsb-release | grep '^DISTRIB_ID' | awk -F=  '{ print $2 }')" | sed 's/\r//'`
                fi
                ;;

            * )
                osInfo=`${SSH_PREFIX} "uname" | sed 's/\r//'`;
                ;;
        esac

        # check on OS CentOS 7
        if [[ ${osType} != "CentOS" || ${osVersion} != "7" ]]; then
            osIssueFound=true
        fi

        resourceIssueFound=false

        availableRAM=`${SSH_PREFIX} "cat /proc/meminfo | grep MemTotal" | awk '{print $2}'`
        availableRAMIssue=false

        availableDiskSpace=`${SSH_PREFIX} "sudo df ${HOME} | tail -1" | awk '{print $2}'`
        availableDiskSpaceIssue=false

        if [[ -z ${availableRAM} || ${availableRAM} < ${MIN_RAM_KB} ]]; then
            resourceIssueFound=true
            availableRAMIssue=true
        fi

        if [[ -z ${availableDiskSpace} || ${availableDiskSpace} < ${MIN_DISK_SPACE_KB} ]]; then
            resourceIssueFound=true
            availableDiskSpaceIssue=true
        fi

        if [[ ${osIssueFound} == true || ${resourceIssueFound} == true ]]; then
            printLn "$(printf "%-43s" "${HOST}" &&  printRed "[NOT OK]")"

            globalNodeIssueFound=true

            osInfoToDisplay=$(printf "%-30s" "${osInfo}")
            if [[ ${osIssueFound} == true ]]; then
                printLn "> DETECTED OS: ${osInfoToDisplay} $(printRed "[NOT OK]")"
                printLn
            fi

            minRAMToDisplay=$(printf "%-15s" "$(printf "%0.2f" "$( m=34; awk -v m=${MIN_RAM_KB} 'BEGIN { print m/1000/1000 }' )") GB")
            availableRAMToDisplay=`${SSH_PREFIX} "cat /proc/meminfo | grep MemTotal" | awk '{tmp = $2/1000/1000; printf"%0.2f",tmp}'`
            availableRAMToDisplay=$(printf "%-11s" "${availableRAMToDisplay} GB")
            RAMStateToDisplay=$([ ${availableRAMIssue} == false ] && echo "$(printGreen "[OK]")" || echo "$(printRed "[NOT OK]")")

            minDiskSpaceToDisplay=$(printf "%-15s" "$(( ${MIN_DISK_SPACE_KB}/1000/1000 )) GB")
            availableDiskSpaceToDisplay=$(( availableDiskSpace /1000/1000 ))
            availableDiskSpaceToDisplay=$(printf "%-11s" "${availableDiskSpaceToDisplay} GB")
            diskStateToDisplay=$([ ${availableDiskSpaceIssue} == false ] && echo "$(printGreen "[OK]")" || echo "$(printRed "[NOT OK]")")

            if [[ ${resourceIssueFound} == true ]]; then
                printLn ">                 RECOMMENDED     AVAILABLE"
                printLn "> RAM             ${minRAMToDisplay} ${availableRAMToDisplay} ${RAMStateToDisplay}"
                printLn "> Disk Space      ${minDiskSpaceToDisplay} ${availableDiskSpaceToDisplay} ${diskStateToDisplay}"
                printLn
            fi
        else
            printLn "$(printf "%-43s" "${HOST}" &&  printGreen "[OK]")"
        fi
    done

    if [[ ${globalNodeIssueFound} == true ]]; then
        printLn "!!! Some nodes do not match requirements.             !!!"
        printLn "!!! See: http://docs.codenvy.com/onprem/#sizing-guide !!!"
        printLn

        if [[ ${SILENT} == true && ${globalOsIssueFound} == true ]]; then
            exit 1;
        fi

        if [[ ${SILENT} != true ]]; then
            pressYKeyToContinue "Proceed?"
            printLn
        fi
    fi
}

doConfigureSystem() {
    nextStep 0 "Configuring system..."

    if [ -d ${DIR} ]; then rm -rf ${DIR}; fi
    mkdir ${DIR}
}

doInstallPackages() {
    nextStep 1 "Installing required packages... [tar ]"
    installPackageIfNeed tar

    nextStep 1 "Installing required packages... [wget]"
    installPackageIfNeed wget

    nextStep 1 "Installing required packages... [unzip]"
    installPackageIfNeed unzip

    nextStep 1 "Installing required packages... [java]"
    installJava
}

doInstallImCli() {
    nextStep 2 "Install the Codenvy installation manager..."
    installIm
}

doDownloadBinaries() {
    nextStep 3 "Downloading Codenvy binaries... "
    OUTPUT=$(executeIMCommand im-download ${ARTIFACT} ${VERSION})
    EXIT_CODE=$?
    echo ${OUTPUT} | sed 's/\[[=> ]*\]//g'  >> install.log
    validateExitCode ${EXIT_CODE}

    executeIMCommand im-download --list-local >> install.log
    validateExitCode $?
}

doInstallCodenvy() {
    for ((STEP=1; STEP<=9; STEP++));  do
        if [ ${STEP} == 9 ]; then
            nextStep $(( $STEP+3 )) "Booting Codenvy... "
        else
            nextStep $(( $STEP+3 )) "Installing Codenvy... "
        fi

        if [ ${CODENVY_TYPE} == "multi" ]; then
            executeIMCommand im-install --step ${STEP} --forceInstall --multi --config ${CONFIG} ${ARTIFACT} ${VERSION} >> install.log
            validateExitCode $?
        else
            executeIMCommand im-install --step ${STEP} --forceInstall --config ${CONFIG} ${ARTIFACT} ${VERSION} >> install.log
            validateExitCode $?
        fi
    done

    nextStep 14 ""

    sleep 2
    pauseTimer
    echo
}

nextStep() {
    pauseTimer

    CURRENT_STEP=$1
    shift

    cursorUp
    cursorUp
    printLn "$@"
    updateProgress ${CURRENT_STEP}

    continueTimer
}

runTimer() {
    updateTimer &
    PROGRESS_PID=$!
}

killTimer() {
    [ ! -z ${PROGRESS_PID} ] && kill -KILL ${PROGRESS_PID}
}

continueTimer() {
    [ ! -z ${PROGRESS_PID} ] && kill -SIGCONT ${PROGRESS_PID}
}

pauseTimer() {
    [ ! -z ${PROGRESS_PID} ] && kill -SIGSTOP ${PROGRESS_PID}
}

updateTimer() {
    for ((;;)); do
        END_TIME=`date +%s`
        DURATION=$(( $END_TIME-$START_TIME))
        M=$(( $DURATION/60 ))
        S=$(( $DURATION%60 ))

        printLn "Elapsed time: "${M}"m "${S}"s"
        cursorUp

        sleep 1
    done
}

updateProgress() {
    CURRENT_STEP=$1
    LAST_STEP=14
    FACTOR=2

    print "Full install ["
    for ((i=1; i<=$CURRENT_STEP*$FACTOR; i++));  do
       echo -n "="
    done
    for ((i=$CURRENT_STEP*$FACTOR+1; i<=$LAST_STEP*$FACTOR; i++));  do
       echo -n " "
    done
    PROGRESS=$(( $CURRENT_STEP*100/$LAST_STEP ))
    echo "] "${PROGRESS}"%"
}

printPostInstallInfo() {
    [ -z ${SYSTEM_ADMIN_NAME} ] && SYSTEM_ADMIN_NAME=`grep admin_ldap_user_name= ${CONFIG} | cut -d '=' -f2`
    [ -z ${SYSTEM_ADMIN_PASSWORD} ] && SYSTEM_ADMIN_PASSWORD=`grep system_ldap_password= ${CONFIG} | cut -d '=' -f2`
    [ -z ${HOST_NAME} ] && HOST_NAME=$(grep host_url\\s*=\\s*.* ${CONFIG} | sed 's/host_url\s*=\s*\(.*\)/\1/')

    printLn
    printLn "Codenvy is ready at http://"${HOST_NAME}
    printLn
    printLn "!!! Set up a DNS entry for Codenvy, or add a hosts rule to your clients: !!!"
    printLn "!!! http://docs.codenvy.com/onprem/installation-bootstrap/#prereq        !!!"
    printLn
    printLn "Admin user name : "${SYSTEM_ADMIN_NAME}
    printLn "Admin password  : "${SYSTEM_ADMIN_PASSWORD}
}

set -e
setRunOptions "$@"
printPreInstallInfo_${CODENVY_TYPE}

runTimer

doConfigureSystem
doInstallPackages
doInstallImCli

set +e

doDownloadBinaries
doInstallCodenvy

printPostInstallInfo
