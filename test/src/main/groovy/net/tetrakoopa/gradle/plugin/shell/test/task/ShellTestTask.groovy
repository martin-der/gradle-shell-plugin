package net.tetrakoopa.gradle.plugin.shell.test.task

import net.tetrakoopa.gradle.plugin.shell.test.ShellTestPluginExtension
import net.tetrakoopa.gradle.plugin.shell.test.ShellTestPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger


class ShellTestTask extends DefaultTask {

	//@Input
	File script

	String testName

	String workingDir = "."

	final OutputRedirect outputRedirect = new OutputRedirect()

	@TaskAction
	def testScript() {

		Project project = getProject()
		def logger = project.logger
		ShellTestPluginExtension shell_test = project.shell_test
		logger.info("Testing '${script.name}'")
		Project topProject = project.getTopProject()

		shell_test.result.executedTestsCount++

		if (workingDir==null) workingDir = project.file(".").absolutePath

		def environmentVariables = [:]
		environmentVariables << project.shell_test.environmentVariables
		environmentVariables.put(ShellTestPlugin.ENVVAR_TEST_RESULTS_DIRECTORY,
			testName != null
				? project.shell_test.resultsDir.absolutePath+"/"+testName
				: project.shell_test.resultsDir.absolutePath
		)

		def execResult = project.exec() {
			it.workingDir = workingDir
			it.environment environmentVariables
			commandLine 'bash', "${script.path}"
			ignoreExitValue true
			standardOutput new LogOutputStream(logger, outputRedirect.standard)
			errorOutput    new LogOutputStream(logger, outputRedirect.error)
		}
		logger.info("  Tested '${script.path}' : $execResult")
		if(execResult.exitValue != 0) {
			shell_test.result.failedTests << this
		}
	}

	class OutputRedirect {
		LogLevel standard = LogLevel.INFO
		LogLevel error = LogLevel.ERROR
	}
}


class LogOutputStream extends ByteArrayOutputStream {

	private final Logger logger
	private final LogLevel level

	LogOutputStream(Logger logger, LogLevel level) {
		this.logger = logger
		this.level = level
	}

	Logger getLogger() {
		return logger
	}

	LogLevel getLevel() {
		return level
	}

	@Override
	void flush() {
		logger.log(level, toString());
		reset()
	}
}
