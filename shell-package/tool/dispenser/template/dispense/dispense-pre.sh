#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'


source "${MDU_SD_INSTALL_TEMP_DIR}/util/log.sh"

[ "x${MDU_LOG_LEVEL:-}" = "x" ] && MDU_LOG_LEVEL=info

log_debug "Working in '$(pwd)'"



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


