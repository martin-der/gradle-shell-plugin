package net.tetrakoopa.gradle.plugin.exception;


public class ShellPackagePluginException extends RuntimeException {
	public ShellPackagePluginException(String message) { super(message); }
	public ShellPackagePluginException(String message, Throwable cause) { super(message, cause); }
}