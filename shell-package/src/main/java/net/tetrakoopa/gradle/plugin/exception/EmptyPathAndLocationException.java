package net.tetrakoopa.gradle.plugin.exception;

public class EmptyPathAndLocationException extends RuntimeException {
	public EmptyPathAndLocationException() {
		this(null);
	}
	public EmptyPathAndLocationException(String forWhat) {
		super("Both a part and a location have been provided"+(forWhat!=null?(" for "+forWhat):""));
	}
}
