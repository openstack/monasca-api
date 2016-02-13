#!/bin/sh
set -x
ME=`whoami`
echo "Running as user: $ME"
MVN=$1
VERSION=$2
BRANCH=$3

check_user() {
    ME=$1
    if [ "${ME}" != "jenkins" ]; then
       echo "\nERROR: Download monasca-common and do a mvn install to install the monasca-commom jars\n" 1>&2
       exit 1
    fi
}

BUILD_COMMON=false
POM_FILE=~/.m2/repository/monasca-common/monasca-common/${VERSION}/monasca-common-${VERSION}.pom
if [ ! -r "${POM_FILE}" ]; then
    check_user ${ME}
    BUILD_COMMON=true
fi

# This should only be done on the stack forge system
if [ "${BUILD_COMMON}" = "true" ]; then
   git clone -b ${BRANCH} https://git.openstack.org/openstack/monasca-common
   cd monasca-common
   ${MVN} clean
   ${MVN} install
fi
