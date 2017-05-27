package net.tetrakoopa.gradle.plugin.shell.test

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.util.ConfigureUtil


class ShellTestPluginExtension {

	public final static String SHELL_TEST_EXTENSION_NAME = "shell_test"

	private Project project

	ShellTestPluginExtension(Project project) {
		this.project = project
	}

	class Result {
		class Check {
			int executedCount
			def failed = []
		}
		final Check check = new Check()
		int executedCount
		def failed = []
	}

	class TestSuite {
		String shunit2Home
		String executable
		String runnerInclude
	}

	class ReturnCode {
		String executionError
		String assertionFailure
	}

	class Check {
		class Naming {
			String prefix
		}
		boolean enabled
		final Naming naming = new Naming()
		File resultsDir
		boolean thowErrorOnBadResult
		def naming(Closure closure) { ConfigureUtil.configure(closure, naming) }
	}

	class Naming {
		boolean removeCommonPrefix
		String prefix
	}

	def environmentVariables = [:]

	final TestSuite testSuite = new TestSuite()
	final ReturnCode returnCode = new ReturnCode()
	final Naming naming = new Naming()
	final Check check = new Check()

	ConfigurableFileCollection testScripts
	File workingDir
	final Result result = new Result()
	File resultsDir
	boolean thowErrorOnBadResult
	ConfigurableFileCollection scripts

	ConfigurableFileCollection scriptFrom(Object... paths) {
		if (scripts == null)
			scripts = project.files(paths)
		else
			scripts.from(paths)
		return scripts
	}

	ConfigurableFileCollection from(Object... paths) {
		if (testScripts == null)
			testScripts = project.files(paths)
		else
			testScripts.from(paths)
		return testScripts
	}

	ShellTestPluginExtension workingDir(Object dir) {
		workingDir = project.file(dir)
		return this
	}

	def testSuite(Closure closure) { ConfigureUtil.configure(closure, testSuite) }
	def returnCode(Closure closure) { ConfigureUtil.configure(closure, returnCode) }
	def naming(Closure closure) { ConfigureUtil.configure(closure, naming) }
	def check(Closure closure) { ConfigureUtil.configure(closure, check) }
	def result(Closure closure) { ConfigureUtil.configure(closure, result) }
}

