#!/usr/bin/env bash

mdu_install_clean_all() {
	: # rm -rf "$MDU_INSTALL_TMP_DIR"
}

MDU_SD_ORIGINAL_WORK_DIR="$(pwd)"

MDU_SD_INSTALL_TEMP_DIR=`mktemp --tmpdir -d mdu-sp-dispenser.XXXXXXXXXXXXXXXXXXXX` || exit 1
trap "mdu_install_clean_all" EXIT

MDU_SD_DECODE_BASE64="base64 --decode"


