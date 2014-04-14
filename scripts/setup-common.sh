#!/bin/bash -e

### Settings

# Repository
dir_cfg="sling-cfg"
dirs=("apps" "libs")
dir_install="${dirs[0]}/install"

# Sling
login="admin:admin"

# Bundles (as maven dependencies)
bundles_common=(
	"org.apache.zookeeper:zookeeper:3.3.6"
	"org.apache.sling:org.apache.sling.devops.common:0.0.1-SNAPSHOT"
	)

### Functions

parse_and_check_maven_artifacts() {
	local jars=()
	for artifact in $@
	do
		local jar=$(parse_maven_artifact ${artifact})
		if ! (ls ${jar} &> /dev/null)
		then
			echo "  ${jar} not found, please ensure all Maven artifacts are in your local repository, exiting"
			echo "  (* hint: execute \"mvn dependency:get -Dartifact=${artifact}\")"
			exit -1
		fi
		jars+=(${jar})
	done
	echo ${jars[@]} # return
}

copy_jars() {
	local url=$1
	shift
	for jar in $@
	do
		echo "  ${jar}"
		curl -u ${login} -T ${jar} "${url}"
	done
}

parse_maven_artifact() {
	local artifact=$1
	local pieces=(${artifact//:/ })
	local groupId=${pieces[0]}
	local artifactId=${pieces[1]}
	local version=${pieces[2]}
	local jar=~/.m2/repository/${groupId//./\/}/${artifactId}/${version}/${artifactId}-${version}.jar
	echo ${jar} # return
}
