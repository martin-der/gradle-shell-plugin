package net.tetrakoopa.gradle.plugin.shell.test.task

import net.tetrakoopa.gradle.plugin.shell.test.ShellTestPlugin
import net.tetrakoopa.gradle.plugin.shell.test.ShellTestPluginExtension

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction

class ShellCheckTask extends DefaultTask {

	//@Input
	File script

	String testName

	String workingDir = "."

	LogLevel errorRedirect = LogLevel.ERROR

	@TaskAction
	def shellcheckScript() {

		Project project = getProject()
		def logger = project.logger
		ShellTestPluginExtension shell_test = project.shell_test
		logger.info("Checking '${script.name}'")

		shell_test.result.check.executedCount++

		if (workingDir==null) workingDir = project.file(".").absolutePath

		File destination = new File(project.shell_test.check.resultsDir.absolutePath+"/"+testName+".txt")
		destination.getParentFile().mkdirs()

		def execResult = project.exec() {
			it.workingDir = workingDir
			commandLine 'shellcheck', "${script.path}"
			ignoreExitValue true
			standardOutput new FileOutputStream(destination)
			errorOutput    new LogOutputStream(logger, errorRedirect)
		}
		logger.info("  Checked '${script.path}' : $execResult")
		if(execResult.exitValue != 0) {
			shell_test.result.check.failed << this
		}
	}

}
