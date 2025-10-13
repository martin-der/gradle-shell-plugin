package net.tetrakoopa.gradle.plugin.exception;

public class BothPathAndLocationDefinedException extends RuntimeException {
	public BothPathAndLocationDefinedException() {
		this(null);
	}
	public BothPathAndLocationDefinedException(String forWhat) {
		super("Both a part and a location have been provided"+(forWhat!=null?(" for "+forWhat):""));
	}
}
