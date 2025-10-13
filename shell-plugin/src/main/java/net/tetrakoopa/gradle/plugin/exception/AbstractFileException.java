package net.tetrakoopa.gradle.plugin.exception;

public class AbstractFileException extends Exception {
	public AbstractFileException(String message, Throwable cause) {
		super(message, cause);
	}
	public AbstractFileException(String message) {
		super(message);
	}
	public AbstractFileException(Throwable cause) {
		super(cause);
	}
}
