
chmod +x install.sh || exit 1


title_short="Installation of $MDU_INSTALL_APPLICATION_LABEL"

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
	exec "$found_term" -title "$title_short" -e "./install.sh"
	return $?
}


#if [ -t 1 ] ; then
if tty -s; then
	./install.sh
else
	install_with_terminal || {
		echo "Unable to run installation script with terminal" >&2
		exit 1
	}
fi

clean_all

exit 0
