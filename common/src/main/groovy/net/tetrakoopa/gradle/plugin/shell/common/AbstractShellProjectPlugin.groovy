package net.tetrakoopa.gradle.plugin.shell.common

import net.tetrakoopa.gradle.plugin.common.AbstractProjectPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class AbstractShellProjectPlugin extends AbstractProjectPlugin implements Plugin<Project> {

	protected void addProjectExtensions(Project project) {
		super.addProjectExtensions(project)
	}

}