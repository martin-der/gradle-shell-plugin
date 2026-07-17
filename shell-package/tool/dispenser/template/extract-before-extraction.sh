
if [ "x${MDU_SD_PERSISTENT_TEMP_FOLDER:-}" != "x" ] ; then
	declare -r MDU_SD_USE_PERSISTENT_TEMP_DIRECTORY=1

	declare -r ROOT_TEMP_DIRECTORY="$(dirname "$(mktemp -u)")"
	declare -r MDU_SD_ABSOLUTE_PERSISTENT_TEMP_DIRECTORY="${ROOT_TEMP_DIRECTORY}/${MDU_SD_PERSISTENT_TEMP_FOLDER}"

	MDU_SD_INSTALL_TEMP_DIR=${MDU_SD_ABSOLUTE_PERSISTENT_TEMP_DIRECTORY}
else
	declare -r MDU_SD_USE_PERSISTENT_TEMP_DIRECTORY=0

	MDU_SD_INSTALL_TEMP_DIR=`mktemp --tmpdir -d mdu-sp-dispenser.XXXXXXXXXXXXXXXXXXXX` || exit 1
fi

declare -r MDU_SD_PERSISTENT_TEMP_DIRECTORY_KEEP_LOCK="${MDU_SD_INSTALL_TEMP_DIR}/keep-directory"

mdu_install_clean_all() {
	if [ ${MDU_SD_USE_PERSISTENT_TEMP_DIRECTORY} -eq 1 ] ; then
		if [ ! -f "${MDU_SD_PERSISTENT_TEMP_DIRECTORY_KEEP_LOCK}" ]; then
			rm -rf "${MDU_SD_INSTALL_TEMP_DIR}"
		fi
	else
		rm -rf "${MDU_SD_INSTALL_TEMP_DIR}"
	fi
}

trap "mdu_install_clean_all" EXIT



if [ ${MDU_SD_USE_PERSISTENT_TEMP_DIRECTORY} -eq 1 ] ; then
	

	if [ -d "x${MDU_SD_ABSOLUTE_PERSISTENT_TEMP_DIRECTORY}" ] ; then
		mdu_sd_execute_dispense "${@}"
		exit $?
	fi

	mkdir "${MDU_SD_ABSOLUTE_PERSISTENT_TEMP_DIRECTORY}"
fi




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


