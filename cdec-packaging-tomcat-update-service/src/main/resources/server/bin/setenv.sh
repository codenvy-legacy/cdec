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

# Environment Variable Prerequisites

if [ -z "${CODENVY_LOCAL_CONF_DIR}" ]; then
    echo "Need to set CODENVY_LOCAL_CONF_DIR"
    exit 1
fi

[ -z "${JAVA_OPTS}" ]  && JAVA_OPTS="-Xms256m -Xmx2G -XX:MaxPermSize=256m -XX:+UseCompressedOops"

GENERAL_OPTS="-Dcodenvy.local.conf.dir=${CODENVY_LOCAL_CONF_DIR} \
              -Dcodenvy.logback.smtp.appender=${CODENVY_LOCAL_CONF_DIR}\logback-smtp-appender.xml \
              -Dcodenvy.logs.dir=${CATALINA_HOME}/logs"

JMX_OPTS="-Dcom.sun.management.jmxremote.authenticate=true \
          -Dcom.sun.management.jmxremote.password.file=${CATALINA_HOME}/conf/jmxremote.password \
          -Dcom.sun.management.jmxremote.access.file=${CATALINA_HOME}/conf/jmxremote.access \
          -Dcom.sun.management.jmxremote.ssl=false"

SECURITY_OPTS="-Djava.security.auth.login.config=${CATALINA_HOME}/conf/jaas.conf"

#REMOTE_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
REMOTE_DEBUG=

export CATALINA_HOME
export CATALINA_TMPDIR
export CODENVY_LOCAL_CONF_DIR
export JAVA_OPTS="$JAVA_OPTS $SECURITY_OPTS $GENERAL_OPTS $JMX_OPTS $REMOTE_DEBUG $JMX_OPTS"

echo "Using LOCAL_CONF_DIR:  $CODENVY_LOCAL_CONF_DIR"
