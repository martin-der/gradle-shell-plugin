package net.tetrakoopa.gradle.plugin.shell.test.task

import net.tetrakoopa.gradle.plugin.shell.test.ShellTestException
import net.tetrakoopa.gradle.plugin.shell.test.ShellTestPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CheckTestsTesultsTask extends DefaultTask {

	@TaskAction
	def showResults() {
		Project project = getProject()
		def logger = project.logger
		ShellTestPluginExtension shell_test = project.shell_test
		def testsCount = shell_test.result.executedTestsCount
		def failedTestsCount = shell_test.result.failedTests.size()
		if (failedTestsCount>0) {
			logger.error("Some test(s) failed :")
			shell_test.result.failedTests.each { logger.error("  - ${it.name}") }
			throw new ShellTestException("$failedTestsCount / $testsCount failed")
		} else
			logger.info("All tests succeeded")
	}
}
