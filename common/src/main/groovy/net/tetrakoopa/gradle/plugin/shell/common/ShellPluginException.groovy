package net.tetrakoopa.gradle.plugin.shell.common

import org.gradle.api.GradleException

class ShellPluginException extends GradleException {
	ShellPluginException(String message) { super(message) }
}
