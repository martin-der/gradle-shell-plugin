package net.tetrakoopa.gradle.plugin.shell.test.task

import net.tetrakoopa.gradle.plugin.shell.test.ShellTestException
import net.tetrakoopa.gradle.plugin.shell.test.ShellTestPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CheckChecksResultsTask extends DefaultTask {

	@TaskAction
	def showResults() {
		Project project = getProject()
		def logger = project.logger
		ShellTestPluginExtension shell_test = project.shell_test
		def checksCount = shell_test.result.check.executedCount
		def failedChecksCount = shell_test.result.check.failed.size()
		if (failedChecksCount>0) {
			logger.error("Some check(s) failed :")
			shell_test.result.check.failed.each { logger.error("  - ${it.name}") }
			def errorMessage = "$failedChecksCount / $checksCount failed"
			if (shell_test.check.thowErrorOnBadResult)
				throw new ShellTestException("Failed check(s) : $failedChecksCount / $checksCount")
		} else
			logger.info("All checks succeeded")
	}
}
