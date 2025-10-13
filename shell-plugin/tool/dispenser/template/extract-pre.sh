#!/usr/bin/env bash

mdu_install_clean_all() {
	# echo "m - Removing '$MDU_INSTALL_TMP_DIR'"
	rm -rf "$MDU_INSTALL_TMP_DIR"
}

MDU_ORIGINAL_WORK_DIR="$(pwd)"

MDU_INSTALL_TMP_DIR=`mktemp --tmpdir -d mdu-installer.XXXXXXXXXXXXXXXXXXXX` || exit 1
trap "mdu_install_clean_all" EXIT

# echo "m - Working in '$MDU_INSTALL_TMP_DIR'"
cd "$MDU_INSTALL_TMP_DIR" || exit 1
