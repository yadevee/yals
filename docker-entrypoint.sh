#!/bin/bash

# usage: file_env VAR [DEFAULT]
#    ie: file_env 'DB_PASSWORD' 'example'
# (will allow for "$DB_PASSWORD_FILE" to fill in the value of
#  "$DB_PASSWORD" from a file, especially for Docker's secrets feature)
file_env() {
  local var="$1"
  local fileVar="${var}_FILE"
  local def="${2:-}"
  if [ "${!var:-}" ] && [ "${!fileVar:-}" ]; then
    echo >&2 "error: both $var and $fileVar are set (but are exclusive)"
    exit 1
  fi
  local val="$def"
  if [ "${!var:-}" ]; then
    val="${!var}"
  elif [ "${!fileVar:-}" ]; then
    val="$(<"${!fileVar}")"
  fi
  export "$var"="$val"
  unset "$fileVar"
}

file_env 'DB_PASSWORD'
file_env 'TELEGRAM_TOKEN'
file_env 'BUGSNAG_TOKEN'
file_env 'APM_TOKEN'
file_env 'DELETE_TOKEN'

JAVA_OPTS=${JAVA_OPTS}

# Remote Debug Support
if [[ -n "${JAVA_DEBUG_PORT}" ]]; then
  export JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:${JAVA_DEBUG_PORT}"
fi
# End Remote Debug Support

# JMX #
if [[ -n "${JAVA_JMX_PORT}" ]]; then
  export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote"
  export JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.hostname=127.0.0.1"
  export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=${JAVA_JMX_PORT}"
  export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.rmi.port=${JAVA_JMX_PORT}"
  export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
  export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.ssl=false"
  export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.local.only=false"
fi
# End JMX #

export JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
export JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
export JAVA_OPTS="$JAVA_OPTS -XX:+UseContainerSupport"
export JAVA_OPTS="$JAVA_OPTS -XX:+AlwaysActAsServerClassMachine"
export JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/dumps"

## Issue 264 (OpenJ9 tuning). Based on https://yals.ee/dUxHlC
export JAVA_OPTS="$JAVA_OPTS -Xgcpolicy:gencon"
export JAVA_OPTS="$JAVA_OPTS -Xquickstart"
export JAVA_OPTS="$JAVA_OPTS -Xtune:virtualized"
export JAVA_OPTS="$JAVA_OPTS -XX:+ClassRelationshipVerifier"
export JAVA_OPTS="$JAVA_OPTS -XX:+TransparentHugePage"

export JAVA_OPTS="$JAVA_OPTS -Xdump:heap"
export JAVA_OPTS="$JAVA_OPTS -Xdump:java"
## End OpenJ9 tuning

# Issues 223 and 237 (APM Support) #
APM_JAR="/apm-agent.jar"
if [[ -s "${APM_JAR}" && -n "${APM_ENV}" && -n "${APM_SERVER}" ]]; then
  export JAVA_OPTS="$JAVA_OPTS -javaagent:${APM_JAR}"
  export JAVA_OPTS="$JAVA_OPTS -Delastic.apm.environment=${APM_ENV}"
  export JAVA_OPTS="$JAVA_OPTS -Delastic.apm.service_name=yalsee"
  export JAVA_OPTS="$JAVA_OPTS -Delastic.apm.application_packages=io.kyberorg.yalsee"
  export JAVA_OPTS="$JAVA_OPTS -Delastic.apm.server_urls=${APM_SERVER}"
  if [ -n "${APM_TOKEN}" ]; then
    export JAVA_OPTS="$JAVA_OPTS -Delastic.apm.secret_token=${APM_TOKEN}"
  fi
fi
# End Issues 223 and 237 (APM Support) #

# Issue 236 (Vaadin Production Mode) #
export JAVA_OPTS="$JAVA_OPTS -Dvaadin.production=true"
# End Issue 236 (Vaadin Production Mode) #

echo "Running java ${JAVA_OPTS} -jar /app/yalsee.jar"

exec java ${JAVA_OPTS} -jar /app/yalsee.jar
