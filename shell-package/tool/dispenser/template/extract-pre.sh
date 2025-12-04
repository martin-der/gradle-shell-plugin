#!/usr/bin/env bash

mdu_install_clean_all() {
	: # rm -rf "$MDU_INSTALL_TMP_DIR"
}

MDU_SD_ORIGINAL_WORK_DIR="$(pwd)"

MDU_SD_INSTALL_TEMP_DIR=`mktemp --tmpdir -d mdu-sp-dispenser.XXXXXXXXXXXXXXXXXXXX` || exit 1
trap "mdu_install_clean_all" EXIT


function mdu_sd_command_exists() {
	which "$1" >/dev/null 2>&1
	return $?
}

mdu_sd_command_exists "base64" && {
	MDU_SD_DECODE_BASE64="base64 --decode"
} || {
	MDU_SD_DECODE_BASE64="mdu_decode_base64"
	{
		echo "Warning :"
		echo "'base64' command was not found. Therefore will use internal base64 tool."
		echo "Uncompressing will be very slow. It is strongly advised to install and make the 'base64' command accessiblle from the PATH"  
	} >&2
}



