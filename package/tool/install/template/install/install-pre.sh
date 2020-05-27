#!/usr/bin/env bash

set -u

source "$(dirname "${BASH_SOURCE[0]}")/script/shell-util.sh"

MDU_LOG_LEVEL=debug

log_debug "Working in '$(pwd)'"


__loglvl2index() {
	local l s="none  NONE  error ERROR warn  WARN  info  INFO  debug DEBUG"
	l=$1 ; l="${s%%$l*}" ; [ "x$l" = "x$s" ] && echo 127 || echo $((${#l}/12))
}
for l in error warn info debug ; do
	[ "x$(type -t "log_${l}")" != xfunction ] && {
		eval "log_${l}() { local lvl=\${MDU_LOG_LEVEL:-\${LOG_LEVEL:-warn}} ; [ \$(__loglvl2index \${lvl}) -lt \$(__loglvl2index ${l}) ] && return 0; echo \"[${l}] \$@\" $([ $(__loglvl2index warn) -ge $(__loglvl2index ${l}) ] && echo ">&2") ; }"
	}
done


INTERRUPT_MUST_BE_TRAPPED=1

clean_exit() {
	if test "${INTERRUPT_MUST_BE_TRAPPED}" -eq 1 ; then
		echo
		echo "Are you sure you want to cancel the installation ?"
		echo "Hit [Control]+[C] again to exit"
		INTERRUPT_MUST_BE_TRAPPED=0
	else
		echo "User requested exit"
		exit 10
	fi
}

trap "clean_exit" SIGINT

function print_application_information() {
	echo "Application '${MDU_INSTALL_APPLICATION_LABEL}'"
	echo "    Name    : ${MDU_INSTALL_APPLICATION_NAME}"
	echo "    Version : ${MDU_INSTALL_APPLICATION_VERSION}"
}

function print_technical_information() {
	echo "Installer"
	echo "    Version   : ${MDU_INSTALLER_VERSION}"
	echo "    Backed by : ${MDU_SHAR_VERSION}"
}


show_version=0
show_help=0
verbosity_level=0
while get_options "verbose|v,version,help|h" option $@ ; do
	[ "x$arg" = "x--" ] && break
	[ "x$arg" = "x--version" ] && {
		print_application_information
		print_technical_information
		exit 0
	}
	echo "Unknown argument '$arg'" >&2
	exit 10
done
