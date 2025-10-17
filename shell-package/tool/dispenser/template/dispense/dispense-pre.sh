#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'


source "$(dirname "${BASH_SOURCE[0]}")/util/log.sh"

MDU_LOG_LEVEL=debug

log_debug "Working in '$(pwd)'"


# INTERRUPT_MUST_BE_TRAPPED=1

# clean_exit() {
# 	if test "${INTERRUPT_MUST_BE_TRAPPED}" -eq 1 ; then
# 		echo
# 		echo "Are you sure you want to cancel the installation ?"
# 		echo "Hit [Control]+[C] again to exit"
# 		INTERRUPT_MUST_BE_TRAPPED=0
# 	else
# 		echo "User requested exit"
# 		exit 10
# 	fi
# }

# trap "clean_exit" SIGINT

function print_application_information() {
	log_info "Application '${MDU_INSTALL_APPLICATION_LABEL}'"
	log_info "    Name    : ${MDU_INSTALL_APPLICATION_NAME}"
	log_info "    Version : ${MDU_INSTALL_APPLICATION_VERSION}"
}

function print_technical_information() {
	log_info "Installer"
	log_info "    Version   : ${MDU_INSTALLER_VERSION}"
	log_info "    Backed by"
	log_info "        shar  : ${MDU_SHAR_VERSION}"
}
