package net.tetrakoopa.gradle.plugin.shell.test

import org.gradle.api.GradleException

class ShellTestException extends GradleException {
	ShellTestException(String message) { super(message) }
}
