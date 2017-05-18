package net.tetrakoopa.gradle.plugin.shell.packaage

import org.gradle.api.GradleException

class ShellPackageException extends GradleException {
	ShellPackageException(String message) { super(message) }
}
