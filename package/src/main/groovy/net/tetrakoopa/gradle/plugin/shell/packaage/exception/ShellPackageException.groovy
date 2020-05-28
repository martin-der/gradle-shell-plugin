package net.tetrakoopa.gradle.plugin.shell.packaage.exception

import org.gradle.api.GradleException

class ShellPackageException extends GradleException {
	ShellPackageException(String message) { super(message) }
	ShellPackageException(String message, Throwable cause) { super(message, cause) }
}
