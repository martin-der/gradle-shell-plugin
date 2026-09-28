package net.tetrakoopa.gradle.plugin.exception;

public class EmptyPathAndLocationException extends ShellPackagePluginException {
	public EmptyPathAndLocationException() {
		this(null);
	}
	public EmptyPathAndLocationException(String forWhat) {
		super("Neither 'path' nor 'location' was provided"+(forWhat!=null?(" for "+forWhat):"")
			+". Provide exactly one of them.");
	}
}
