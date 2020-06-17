package net.tetrakoopa.gradle.plugin.shell.test.task

import net.tetrakoopa.gradle.plugin.shell.test.ShellTestPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class RunTestsResultsTask extends DefaultTask {

	@TaskAction
	def showResults() {
		Project project = getProject()
		def logger = project.logger
		ShellTestPluginExtension shell_test = project.shell_test
		def testsCount = shell_test.result.executedCount
		def failedTestsCount = shell_test.result.failed.size()
		if (failedTestsCount>0) {
			logger.info("${failedTestsCount}/${testsCount} test(s) failed :")
			shell_test.result.failed.each { logger.error("  - ${it.name}") }
		} else
			logger.info("All tests succeeded")
	}
}
