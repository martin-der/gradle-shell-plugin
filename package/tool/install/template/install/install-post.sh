#@IgnoreInspection BashAddShebang

__installer_properties="$(cat "config")"

ui_type="$(properties_find "ui.type" <<< "$__installer_properties")"
ui_theme="$(properties_find "ui.theme" <<< "$__installer_properties")"
installer_prefix_alternatives="$(properties_find "prefix.alternatives" <<< "$__installer_properties")"
installer_prefix_alternatives="$(expand_vars "${installer_prefix_alternatives}")"

log_debug "installer_prefix_alternatives = '$installer_prefix_alternatives'"

display_banner() {
	if [ "x${banner_to_display}" = "x" ]; then
		echo "Installation of '${project_label}' (${project_version})"
		return 0
	fi
	cat "${banner_to_display}"
	echo
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
	if [ "x$readme_to_show" = "x" ]; then
		return 0
	fi
	while true; do
		read -p "Do you want to view the 'README'? [Yn] " yn
		case $yn in
			[Nn] ) break ;;
			* )
				which less >/dev/null && {
					less "$readme_to_show" || return 1
					break
				}
				which more >/dev/null && {
					more "$readme_to_show" || return 1
					break
				}
				cat "$readme_to_show" || return 1
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

# -----------------------
# |      Execution      |
# -----------------------

display_banner

show_readme


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
