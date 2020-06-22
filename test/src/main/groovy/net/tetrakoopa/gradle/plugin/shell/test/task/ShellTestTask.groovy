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

	String testRelativePath

	String workingDir = "."

	final OutputRedirect outputRedirect = new OutputRedirect()

	@TaskAction
	def testScript() {

		Project project = getProject()
		def logger = project.logger
		ShellTestPluginExtension shell_test = project.shell_test
		logger.info("Testing '${script.name}'")
		Project topProject = project.getTopProject()

		shell_test.result.executedCount++

		if (workingDir==null) workingDir = project.file(".").absolutePath

		def environmentVariables = [:]
		environmentVariables << shell_test.environmentVariables
		environmentVariables.put(ShellTestPlugin.ENVVAR_TEST_GENERATED_DIRECTORY,
			testName != null
				? shell_test.output.generatedDirectory.absolutePath+"/"+testName
				: shell_test.output.generatedDirectory.absolutePath
		)
		environmentVariables.put(ShellTestPlugin.ENVVAR_TEST_NAME, testName == null ? "" : testName)
		environmentVariables.put(ShellTestPlugin.ENVVAR_TEST_RELATIVE_PATH, testRelativePath)

		shell_test.output.logDirectory.mkdirs()

		final File logDirectory = new File(testName != null
			? shell_test.output.logDirectory.absolutePath+"/"+testName
			: shell_test.output.logDirectory.absolutePath)
		logDirectory.mkdirs()
		final logOut = new FileOutputStream(new File(logDirectory, 'out.log'))
		final logErr = new FileOutputStream(new File(logDirectory, 'err.log'))

		def execResult = project.exec() {
			it.workingDir = workingDir
			it.environment environmentVariables
			commandLine 'bash', "${script.path}"
			ignoreExitValue true
			standardOutput new TeeOutputStream(logOut, new LogOutputStream(logger, outputRedirect.standard))
			errorOutput    new TeeOutputStream(logErr, new LogOutputStream(logger, outputRedirect.error))
		}
		logger.info("  Tested '${script.path}' : $execResult")
		if(execResult.exitValue != 0) {
			shell_test.result.failed << this
		}
	}

	class OutputRedirect {
		LogLevel standard = LogLevel.INFO
		LogLevel error = LogLevel.ERROR
	}
}


class TeeOutputStream extends OutputStream {
	private OutputStream left;
	private OutputStream right;

	TeeOutputStream(OutputStream left, OutputStream right) {
		this.left = left;
		this.right = right;
	}

	void close() throws IOException {
		try {
			left.close();
		} finally {
			right.close();
		}
	}

	void flush() throws IOException {
		left.flush();
		right.flush();
	}

	void write(byte[] b) throws IOException {
		left.write(b);
		right.write(b);
	}

	void write(byte[] b, int off, int len) throws IOException {
		left.write(b, off, len);
		right.write(b, off, len);
	}

	void write(int b) throws IOException {
		left.write(b);
		right.write(b);
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
