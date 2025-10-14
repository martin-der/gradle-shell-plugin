#@IgnoreInspection BashAddShebang


declare -i has_launcher
[ "x${mdu_sp_executable_reactor_script}" != "x" ] && has_launcher=1 || has_launcher=0


print_usage() {
	local title
	[ "x${mdu_sp_package_version}" != "x" ] \
		&& title="Installer / Launcher for ${mdu_sp_package_label}:${mdu_sp_package_version}" \
		|| title="Installer / Launcher for ${mdu_sp_package_label}"
	echo "${title}"
	echo "Usage :"
	echo "  ${MDU_ROOT_EXECUTION_ARCHIVE} install"
	[ ${has_launcher} -ne 0 ] && {
		echo "  ${MDU_ROOT_EXECUTION_ARCHIVE} launch"
	}
	echo "  ${MDU_ROOT_EXECUTION_ARCHIVE} --help"
}


action=

declare -r argument_action_mode="${1:-}"
shift || true


[ "x${argument_action_mode}" = "x-h" -o "x${argument_action_mode}" = "x--help" ] && {
	print_usage
	exit 0
}

[ "x${argument_action_mode,,}" = "xinstall" ] && {
	action=INSTALL
}
[ ${has_launcher} -ne 0 ] && {
	[ "x${argument_action_mode,,}" = "xlaunch" ] && {
		action=LAUNCH
	}
}


declare -r usage_proposal_text="Type
    ${MDU_ROOT_EXECUTION_ARCHIVE} -h
to get help"


[ "x${action}" = "x" ] && {
	[ ${has_launcher} -ne 0 ] && {
		log_error "First parameter must be 'launch' or 'install' ( or '--help' )."
	} || {
		log_error "First parameter must be 'install' ( or '--help' )."
	}
	log_input_with_prefix "  ${RED}|${COLOR_RESET} " <<< "${usage_proposal_text}"
	exit 1
}

show_version=0
show_help=0
verbosity_level=0
# while get_options "verbose|v,version,help|h" option $@ ; do
# 	[ "x$arg" = "x--" ] && break
# 	[ "x$arg" = "x--version" ] && {
# 		print_application_information
# 		print_technical_information
# 		exit 0
# 	}
# 	echo "Unknown argument '$arg'" >&2
# 	exit 10
# done

# OPTS=$(getopt -o c:hvp: --long config:,help,verbose,port: -n'serveur.sh' -- "$@")

# if [ $? -ne 0 ]; then
#   echo "Failed to parse options" >&2
#   exit 1
# fi

# eval set -- "$OPTS"


# __installer_properties="$(cat "config")"

# ui_type="$(properties_find "ui.type" <<< "$__installer_properties")"
# ui_theme="$(properties_find "ui.theme" <<< "$__installer_properties")"
# installer_prefix_alternatives="$(properties_find "prefix.alternatives" <<< "$__installer_properties")"
# installer_prefix_alternatives="$(expand_vars "${installer_prefix_alternatives}")"

# log_debug "installer_prefix_alternatives = '$installer_prefix_alternatives'"

display_banner() {
	if [ "${mdu_sp_show_banner}" -ne 0 ]; then
		cat "${MDU_DISPENSER_DIRECTORY}/resource/banner.txt"
		echo
	fi
}

get_install_location() {
   local index=0
   local wanted=$1
   [ $wanted -eq $index ] && { echo -n "$HOME/bin" ; return 0 ; }
   index=$(($index+1))
   [ $wanted -eq $index ] && { echo -n "/usr/local/bin" ; return 0 ; }
   index=$(($index+1))
   return 1
}
get_install_locations_count() {
   local count=0
   while get_install_location $count >/dev/null; do
      count=$(($count+1))
   done
   echo $count
}

print_install_locations() {
   local index=0
   local location
   echo "Available locations are :"
   while location="$(get_install_location $index)" ; do
      index=$(($index+1))
      echo "$index) '$location'"
   done
   index=$(($index+1))
   echo "${index}) a custom location..." ;
}

install_scripts() {
	local install_dir="$1"
	local target_name
	local target
	mkdir -p "${install_dir}" || return 1
	while IFS=$'\n' read f ; do
		test "x${f}" == "x" && {
			log_warn "No file to install"
			return 1
		}
		target_name="$(basename "$f")"
		target="${install_dir}/${target_name}"
		log_debug "Copy '${f}' => '${target}'"
   		cp -r "${f}" "${target}" || return 1
	done <<< "$( find ./content -mindepth 1 -maxdepth 1 )"
}

show_readme() {
	if [ ${mdu_sp_show_readme} -eq 0 ]; then
		return 0
	fi
	local file="${MDU_DISPENSER_DIRECTORY}/resource/README.md"
	while true; do
		read -p "Do you want to view the 'README'? [Yn] " yn
		case $yn in
			[Nn] ) break ;;
			* )
				which less >/dev/null && {

					less "${file}" || return 1
					break
				}
				which more >/dev/null && {
					more "${file}" || return 1
					break
				}
				cat "${file}" || return 1
				return 0 ;;
		esac
	done
	return 0
}

execute_user_script() {
	if [ "x$user_script_to_execute" = "x" ]; then
		return 0
	fi
	while true; do
		read -p "$user_script_question [yn] " yn
		case $yn in
			[Yy]* ) break ;;
			[Nn]* ) return 0 ;;
			* ) echo "Please answer Yes or No." ;;
		esac
	done
	sh "$user_script_to_execute"
	local result=$?
	if [ $result -ne 0 ] ; then
		log_error "User script exited with ${result}"
	fi
	return $?
}

# ---------------------------
# |      L A U N C H        |
# ---------------------------

if [ ${action} = 'LAUNCH' ]; then

	log_debug cd "${MDU_ORIGINAL_WORK_DIR}"
	cd "${MDU_ORIGINAL_WORK_DIR}"

	source "${MDU_DISPENSER_DIRECTORY}/resource/environment.sh"

	log_debug "execute 'content/content/${mdu_sp_executable_reactor_script}'"
	"${MDU_DISPENSER_DIRECTORY}/content/${mdu_sp_executable_reactor_script}"
	exit $?
fi

# ---------------------------
# |      I N S T A L L      |
# ---------------------------

display_banner

log_info "Installation of '${mdu_sp_package_label}' (${mdu_sp_package_version})"

show_readme

exit 99

install_with_terminal() {
	local KNOWN_TERMS="xterm konsole gnome-terminal rxvt dtterm eterm Eterm xfce4-terminal lxterminal kvt aterm terminology"

	[ x"$DISPLAY" != x -a "x$term_loop" = x ] || {
		echo "No XDisplay available ( 'term_loop' = '$xterm_loop' )" >&2
		return 1
	}
	xset q > /dev/null 2>&1 || {
		echo "'xset q' failed" >&2
		return 1
	}

	local tested_term found_term=
	for tested_term in ${KNOWN_TERMS}; do
		if type "$tested_term" >/dev/null 2>&1; then
			found_term="$tested_term"
			break
		fi
	done
	[ "x$found_term" = "x" ] && {
		echo "No terminal found" >&2
		return 1
	}
	exec "$found_term" -title "$title_short" -e "./dispense.sh"
	return $?
}

#if [ -t 1 ] ; then
# if tty -s; then
	# ./install.sh
# else
# 	install_with_terminal || {
# 		echo "Unable to run installation script with terminal" >&2
# 		exit 1
# 	}
# fi


install_location_count=$(get_install_locations_count)
install_location_choices_count=$(($install_location_count+1))
while true; do
	print_install_locations
	read -p "Where do you want to install scripts? " install_choice
	case $install_choice in
		''|*[!0-9]*)
			echo "Please answer a number between 1 and $install_location_choices_count."
			;;
		*)
			[ $install_choice -gt 0 -a $install_choice -le $install_location_choices_count ] && {
				break
			} || {
				echo "Please answer a number between 1 and $install_location_choices_count."
			}
			;;
	esac
done

install_choice=$(($install_choice-1))
[ $install_choice -lt $install_location_count ] && {
	install_location="$(get_install_location $install_choice)"
} || {
	while true ; do
		read -p "Choose directory to install scripts in : " install_location
		[ "x$install_choice" != "x" ] && break
	done
}
install_location="${install_location%/}"
log_info "Install in : '$install_location'"

install_scripts "$install_location" || {
	log_error "Failed to install scripts" >&2
	read dummy
	exit 1
}


execute_user_script
