package net.tetrakoopa.gradle.plugin.shell.test.exception

import net.tetrakoopa.gradle.plugin.common.exception.PluginException
import net.tetrakoopa.gradle.plugin.shell.test.ShellTestPlugin

class ShellTestPluginException extends PluginException  {
	ShellTestPluginException(String message) { super(ShellTestPlugin.ID, message) }
	ShellTestPluginException(String message, Throwable cause) { super(ShellTestPlugin.ID, message, cause) }
}
