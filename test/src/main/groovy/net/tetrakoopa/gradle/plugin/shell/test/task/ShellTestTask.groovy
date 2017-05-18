package net.tetrakoopa.gradle.plugin.shell.test.task

import net.tetrakoopa.gradle.plugin.shell.test.ShellTestPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger


class ShellTestTask extends DefaultTask {

	//@Input
	File script

	String workingDir = "."

	final OutputRedirect outputRedirect = new OutputRedirect()

	@TaskAction
	def testScript() {

		Project project = getProject()
		def logger = project.logger
		ShellTestPluginExtension shell_test = project.shell_test
		logger.info("Testing '${script.name}'")
		Project topProject = project.getTopProject()
		File toolsResourcesDir = topProject.file("${topProject.buildDir}/net.tetrakoopa.shell-test/tool")

		shell_test.result.executedTestsCount++

		if (workingDir==null) workingDir = project.file(".").absolutePath

		def execResult = project.exec() {
			it.workingDir = workingDir
			it.environment project.shell_test.environmentVariables
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
