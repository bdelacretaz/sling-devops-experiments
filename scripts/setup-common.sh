#!/bin/bash -e

### Settings

# Repository
dir_cfg="sling-cfg"
dirs=("apps" "libs")
dir_install="${dirs[0]}/install"

# Sling
login="admin:admin"

### Functions

copy_jars() {
	local url=$1
	shift
	for jar in $@
	do
		echo "  ${jar}"
		curl -sSu ${login} -T ${jar} "${url}"
	done
}
