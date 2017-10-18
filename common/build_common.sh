#!/bin/sh
set -x
ME=`whoami`
echo "Running as user: $ME"
MVN=$1
VERSION=$2
BRANCH=$3

BUILD_COMMON=false
POM_FILE=~/.m2/repository/monasca-common/monasca-common/${VERSION}/monasca-common-${VERSION}.pom
if [ ! -r "${POM_FILE}" ]; then
    BUILD_COMMON=true
fi

# This should only be done on the stack forge system
if [ "${BUILD_COMMON}" = "true" ]; then
   git clone -b ${BRANCH} https://git.openstack.org/openstack/monasca-common --depth 1
   cd monasca-common
   ${MVN} clean
   ${MVN} install
fi
