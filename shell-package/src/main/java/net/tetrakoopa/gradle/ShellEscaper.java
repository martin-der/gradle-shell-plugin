package net.tetrakoopa.gradle;

import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

/**
 * Turns arbitrary user-supplied text into a value that is safe to paste verbatim into a
 * generated bash script.
 *
 * <p>Every value the plugin writes into {@code dispense.sh} or into a file that is later
 * {@code source}d by the generated installer goes through here. Without this, a value such as
 * {@code "Foo Bar Frenzy"} silently loses everything after the first space (bash reads
 * {@code declare -r X=Foo Bar} as two arguments) and a value such as {@code "a; rm -rf /"}
 * injects an extra command into the shipped installer.
 */
@UtilityClass
public class ShellEscaper {

	/**
	 * Characters that need no quoting at all in an unquoted bash word.
	 * Deliberately conservative: anything not listed gets single-quoted.
	 */
	private static final Pattern SAFE_UNQUOTED = Pattern.compile("[A-Za-z0-9_@%+=:,./^-]+");

	/**
	 * True when {@code value} can be written to a script as-is.
	 */
	public static boolean isSafeUnquoted(String value) {
		return value != null && !value.isEmpty() && SAFE_UNQUOTED.matcher(value).matches();
	}

	/**
	 * Renders {@code value} as a single, fully quoted bash word.
	 *
	 * <p>Values made only of "safe" characters are returned unchanged so that generated scripts
	 * stay readable and so that existing expectations on the generated text keep holding.
	 * Everything else is wrapped in single quotes, with embedded single quotes escaped the
	 * portable way: {@code '} becomes {@code '\''}.
	 *
	 * @return {@code null} for {@code null} input, so callers can distinguish "unset" from "empty"
	 */
	public static String quote(String value) {
		if (value == null) {
			return null;
		}
		if (isSafeUnquoted(value)) {
			return value;
		}
		return "'" + value.replace("'", "'\\''") + "'";
	}

	/**
	 * Escapes {@code value} for use <em>inside</em> an existing double-quoted string.
	 *
	 * <p>Only {@code \}, {@code "}, {@code $} and a backtick carry meaning there; newlines and
	 * carriage returns are rejected outright because a generated script cannot carry them inside a
	 * single line, and silently truncating would reintroduce exactly the class of bug this class
	 * exists to prevent.
	 *
	 * @throws IllegalArgumentException if the value contains a line break
	 */
	public static String escapeForDoubleQuotes(String value) {
		if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
			throw new IllegalArgumentException(
				"'" + value + "' contains a line break and cannot be embedded in a single shell word");
		}
		final StringBuilder result = new StringBuilder(value.length() + 8);
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			switch (c) {
				case '\\':
				case '"':
				case '$':
				case '`':
					result.append('\\');
					break;
				default:
					break;
			}
			result.append(c);
		}
		return result.toString();
	}
}
