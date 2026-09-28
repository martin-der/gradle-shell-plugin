package net.tetrakoopa.gradle.plugin.exception;

import org.gradle.api.InvalidUserDataException;

/**
 * Base class for every error caused by the way the user configured the plugin.
 *
 * <p>Extends {@link InvalidUserDataException} so that Gradle reports it as a clean
 * "What went wrong" block instead of an internal error with a Java stack trace.
 */
public class ShellPackagePluginException extends InvalidUserDataException {
	public ShellPackagePluginException(String message) { super(message); }
	public ShellPackagePluginException(String message, Throwable cause) { super(message, cause); }
}
