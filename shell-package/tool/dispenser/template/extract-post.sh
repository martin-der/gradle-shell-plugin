
if [ ${MDU_SD_USE_PERSISTENT_TEMP_DIRECTORY} -eq 1 ] ; then
	touch "${MDU_SD_PERSISTENT_TEMP_DIRECTORY_KEEP_LOCK}"
fi


chmod +x "${MDU_SD_INSTALL_TEMP_DIR}/dispense.sh" || exit 1

mdu_sd_execute_dispense "${@}"

exit $?
