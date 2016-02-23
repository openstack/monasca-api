#!/bin/bash
set -x
env
# Download maven 3 if the system maven isn't maven 3
VERSION=`mvn -v | grep "Apache Maven 3"`
if [ -z "${VERSION}" ]; then
   curl http://archive.apache.org/dist/maven/binaries/apache-maven-3.2.1-bin.tar.gz > apache-maven-3.2.1-bin.tar.gz
   tar -xvzf apache-maven-3.2.1-bin.tar.gz
   MVN=${PWD}/apache-maven-3.2.1/bin/mvn
else
   MVN=mvn
fi

# Get the expected common version
COMMON_VERSION=$1
# Get rid of the version argument
shift

# Get rid of the java property name containing the args
shift

RUN_BUILD=false
for ARG in $*; do
   if [ "$ARG" = "package" ]; then
       RUN_BUILD=true
   fi
   if [ "$ARG" = "install" ]; then
       RUN_BUILD=true
   fi
done

if [ $RUN_BUILD = "true" ]; then
    if [ ! -z "$ZUUL_BRANCH" ]; then
        BRANCH=${ZUUL_BRANCH}
    else
        BRANCH=${ZUUL_REF}
    fi

    ( cd common; ./build_common.sh ${MVN} ${COMMON_VERSION} ${BRANCH} )
    RC=$?
    if [ $RC != 0 ]; then
        exit $RC
    fi
fi

# Invoke the maven 3 on the real pom.xml
( cd java; ${MVN} -DgitRevision=`git rev-list HEAD --max-count 1 --abbrev=0 --abbrev-commit` $* )

RC=$?

# Copy the jars where the publisher will find them
if [ $RUN_BUILD = "true" ]; then
   if [ ! -L target ]; then
      ln -sf java/target target
   fi
fi

rm -fr apache-maven-3.2.1*
exit $RC
