package net.tetrakoopa.gradle.plugin.shell.packaage.exception

import net.tetrakoopa.gradle.plugin.common.exception.PluginException

class ShellPackagePluginException extends PluginException {
	ShellPackagePluginException(String message) { super(ShellPackagePlugin.ID, message) }
	ShellPackagePluginException(String message, Throwable cause) { super(ShellPackagePlugin.ID, message, cause) }
}
