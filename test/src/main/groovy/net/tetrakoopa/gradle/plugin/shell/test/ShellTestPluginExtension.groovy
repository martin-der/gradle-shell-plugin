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
		private final Project project
		Check(Project project) { this.project = project }
		class Naming {
			String prefix
			boolean removeSuffix
		}
		final Naming naming = new Naming()
		File resultsDir
		def resultsDir ( String file) { this.resultsDir = file.startsWith('/') ? file : new File(project.buildDir, file) }
		def resultsDir ( File file) { this.resultsDir = file }
		def naming(Closure closure) { ConfigureUtil.configure(closure, naming) }
	}

	class Naming {
		boolean removeCommonPrefix
		String prefix
		boolean removeSuffix
	}

	class Output {
		File generatedDirectory
		def generatedDirectory ( String file) { this.generatedDirectory = file.startsWith('/') ? file : new File(project.buildDir, file) }
		def generatedDirectory ( File file) { this.generatedDirectory = file }
		File logDirectory
		def logDirectory ( String file) { this.logDirectory = file.startsWith('/') ? file : new File(project.buildDir, file) }
		def logDirectory ( File file) { this.logDirectory = file }
	}

	def environmentVariables = [:]

	final TestSuite testSuite = new TestSuite()
	final ReturnCode returnCode = new ReturnCode()
	final Naming naming = new Naming()
	Check check

	ConfigurableFileCollection testScripts

	ConfigurableFileCollection from(Object... paths) {
		if (testScripts == null)
			testScripts = project.files(paths)
		else
			testScripts.from(paths)
		return testScripts
	}

	File workingDir
	final Result result = new Result()
	File outputDirectory
	def outputDirectory ( String file) { this.outputDirectory = file.startsWith('/') ? file : new File(project.buildDir, file) }
	def outputDirectory ( File file) { this.outputDirectory = file }
	final Output output = new Output()

	ConfigurableFileCollection scripts

	ConfigurableFileCollection scriptFrom(Object... paths) {
		if (scripts == null)
			scripts = project.files(paths)
		else
			scripts.from(paths)
		return scripts
	}

	ShellTestPluginExtension workingDir(Object dir) {
		workingDir = project.file(dir)
		return this
	}

	def testSuite(Closure closure) { ConfigureUtil.configure(closure, testSuite) }
	def returnCode(Closure closure) { ConfigureUtil.configure(closure, returnCode) }
	def naming(Closure closure) { ConfigureUtil.configure(closure, naming) }
	def check(Closure closure) {
		check = new Check(project)
		ConfigureUtil.configure(closure, check)
	}
	def output(Closure closure) { ConfigureUtil.configure(closure, output) }
	def result(Closure closure) { ConfigureUtil.configure(closure, result) }
}

