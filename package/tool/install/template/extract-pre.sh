#!/usr/bin/env bash

clean_all() {
	echo "m - Removing '$MDU_INSTALL_TMP_DIR'"
	rm -rf "$MDU_INSTALL_TMP_DIR"
}

MDU_INSTALL_TMP_DIR=`mktemp --tmpdir -d mdu-installer.XXXXXXXXXXXXXXXXXXXX` || exit 1
trap "clean_all" EXIT

echo "m - Working in '$MDU_INSTALL_TMP_DIR'"
cd "$MDU_INSTALL_TMP_DIR" || exit 1
