#!/bin/sh
set -x
ME=`whoami`
echo "Running as user: $ME"
VERSION=$1

check_user() {
    ME=$1
    if [ "${ME}" != "jenkins" ]; then
       echo "\nERROR: Download monasca-common and do a mvn install to install the monasca-commom jars\n" 1>&2
       exit 1
    fi
}

# TODO: Grep these out of the pom itself rather than have them explicitly listed
JARS="
monasca-common-influxdb
monasca-common-model
monasca-common-persistence
monasca-common-util
monasca-common-kafka
monasca-common-middleware
monasca-common-testing
"
for JAR in $JARS; do
    JARFILE=~/.m2//repository/monasca-common/${JAR}/${VERSION}/${JAR}-${VERSION}.jar
    if [ ! -r "$JARFILE" ]; then
        check_user ${ME}
        # Download it from stackforge
        FILE=`basename $JARFILE`
        curl http://tarballs.openstack.org/ci/monasca-common/${FILE} > ${FILE}
        # Upload into the local repository
    POM=META-INF/maven/monasca-common/${JAR}/pom.xml
    jar -xvf ${FILE} ${POM}
    TMPFILE=pom.$$
        sed -e "s/\${computedVersion}/${VERSION}/" ${POM} > ${TMPFILE}
    mv ${TMPFILE} ${POM}
    mvn install:install-file -Dfile=${FILE} -DgroupId=monasca-common \
            -DartifactId=${JAR} -Dversion=${VERSION} -Dpackaging=jar -DpomFile=${POM}
    fi
done

POM_FILE=~/.m2/repository/monasca-common/monasca-common/${VERSION}/monasca-common-${VERSION}.pom
if [ ! -r "${POM_FILE}" ]; then
    check_user ${ME}
    TMPDIR=pom_tmp.$$
    mkdir -p ${TMPDIR}
    curl https://raw.githubusercontent.com/stackforge/monasca-common/master/java/pom.xml > ${TMPDIR}/pom.xml
    mvn install:install-file -DgroupId=monasca-common -DartifactId=monasca-common -Dversion=${VERSION} -Dpackaging=pom -Dfile=${TMPDIR}/pom.xml
    rm -fr ${TMPDIR}
fi

TEST_JARS="
monasca-common-dropwizard
"
for TEST_JAR in $TEST_JARS; do
    JARFILE=~/.m2//repository/monasca-common/${TEST_JAR}/${VERSION}/${TEST_JAR}-${VERSION}-tests.jar
    if [ ! -r "$JARFILE" ]; then
        check_user ${ME}
        # Download it from stackforge
        FILE=`basename $JARFILE`
        curl http://tarballs.openstack.org/ci/monasca-common/${FILE} > ${FILE}
        # Upload into the local repository
    POM=META-INF/maven/monasca-common/${TEST_JAR}/pom.xml
    jar -xvf ${FILE} ${POM}
    TMPFILE=pom.$$
        sed -e "s/\${computedVersion}/${VERSION}/" ${POM} > ${TMPFILE}
    mv ${TMPFILE} ${POM}
        mvn install:install-file -Dfile=${FILE} -DgroupId=monasca-common \
            -DartifactId=${TEST_JAR} -Dversion=${VERSION} -Dpackaging=test-jar -DpomFile=${POM}
    fi
done
