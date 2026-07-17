#!/usr/bin/env bash


MDU_SD_ORIGINAL_WORK_DIR="$(pwd)"


function mdu_sd_command_exists() {
	which "$1" >/dev/null 2>&1
	return $?
}

function mdu_sd_execute_dispense() {

	export MDU_ROOT_EXECUTION_ARCHIVE="${0}"
	export MDU_SD_INSTALL_TEMP_DIR
	export MDU_SD_USE_PERSISTENT_TEMP_DIRECTORY
	export MDU_SD_ABSOLUTE_PERSISTENT_TEMP_DIRECTORY
	export MDU_SD_PERSISTENT_TEMP_DIRECTORY_KEEP_LOCK
	# export MDU_ORIGINAL_WORK_DIR

	"${MDU_SD_INSTALL_TEMP_DIR}/dispense.sh" "${@}"

	return $?
}

