package net.tetrakoopa.gradle.plugin.exception;

public class BothPathAndLocationDefinedException extends ShellPackagePluginException {
	public BothPathAndLocationDefinedException() {
		this(null);
	}
	public BothPathAndLocationDefinedException(String forWhat) {
		super("Both 'path' and 'location' were provided"+(forWhat!=null?(" for "+forWhat):"")
			+". They are mutually exclusive: keep only the one you meant.");
	}
}
