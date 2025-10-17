

chmod +x dispense.sh || exit 1


title_short="Installation of $MDU_INSTALL_APPLICATION_LABEL"

export MDU_ROOT_EXECUTION_ARCHIVE="${0}"
export MDU_DISPENSER_DIRECTORY="${MDU_INSTALL_TMP_DIR}"
export MDU_DISPENSER_CONTENT_DIRECTORY="${MDU_DISPENSER_DIRECTORY}/content"
export MDU_ORIGINAL_WORK_DIR


./dispense.sh "${@}"


exit 0
