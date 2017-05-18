package net.tetrakoopa.gradle.plugin.shell.common

import org.gradle.api.Plugin
import org.gradle.api.Project

class ShellPlugin implements Plugin<Project> {

	void apply(Project project) {
		project.plugins.with {
			apply 'net.tetrakoopa.shell-package'
			apply 'net.tetrakoopa.shell-test'
		}
	}
}
