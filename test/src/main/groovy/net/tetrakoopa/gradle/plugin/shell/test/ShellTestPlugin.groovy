package net.tetrakoopa.gradle.plugin.shell.test

import net.tetrakoopa.gradle.plugin.shell.common.AbstractShellProjectPlugin
import net.tetrakoopa.gradle.plugin.shell.test.task.CheckChecksResultsTask
import net.tetrakoopa.gradle.plugin.shell.test.task.CheckTestsResultsTask
import net.tetrakoopa.gradle.plugin.shell.test.task.ShellTestTask
import net.tetrakoopa.gradle.plugin.shell.test.task.ShellCheckTask
import net.tetrakoopa.poignee.bundledresources.BundledResourcesPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

import static ShellTestPluginExtension.SHELL_TEST_EXTENSION_NAME

class ShellTestPlugin extends AbstractShellProjectPlugin implements Plugin<Project> {

	public static final String ID = "net.tetrakoopa.shell-test"

	private static final String GRADLE_LIFECYCLE_TEST_TASK = 'check'

	public static final String LOGGING_PREFIX = "Shell-Test : "

	public static final String ALL_TESTS_RESULT_TASK_NAME = "shell-checktests"
	public static final String ALL_TESTS_TASK_NAME = "shell-test"

	public static final String ALL_CHECKS_RESULT_TASK_NAME = "shell-checkchecks"
	public static final String ALL_CHECKS_TASK_NAME = "shell-check"

	public static final String ENVVAR_TEST_RESULTS_DIRECTORY = "MDU_SHELLTEST_TEST_RESULTS_DIRECTORY"
	public static final String ENVVAR_TEST_NAME = "MDU_SHELLTEST_TEST_NAME"

	private static final def FILENAME_SUFFIX_REGEX = ~/(?i)\.\w+$/

	private int stringsGreatestCommonPrefixLength(String a, String b) {
		int minLength = Math.min(a.length(), b.length())
		for (int i = 0; i < minLength; i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return i
			}
		}
		return minLength
	}
	private void prepareEnvironment(Project project) {
		project.shell_test.environmentVariables['MDU_SHELLTEST_TESTUNIT_SHUNIT2_EXEC'] = project.shell_test.testSuite.executable
		project.shell_test.environmentVariables['MDU_SHELLTEST_PROJECT_DIRECTORY'] = project.projectDir
		project.shell_test.environmentVariables['MDU_SHELLTEST_TEST_EXECUTION_ERROR_EXIT_CODE'] = project.shell_test.returnCode.executionError
		project.shell_test.environmentVariables['MDU_SHELLTEST_TEST_ASSERTION_FAILURE_EXIT_CODE'] = project.shell_test.returnCode.assertionFailure
		project.shell_test.environmentVariables['MDU_SHELLTEST_RESULTS_DIRECTORY'] = project.shell_test.resultsDir
		project.shell_test.environmentVariables['MDU_SHELLTEST_TESTUNIT_RUNNER_INCLUDE'] = project.shell_test.testSuite.runnerInclude

		//project.shell_test.environmentVariables['MDU_SHELLTEST_TESTUNIT_SOURCE_DIRECTORY'] = project.shell_test.testSuite.executable
		//project.shell_test.environmentVariables['MDU_SHELLTEST_TESTUNIT_TEST_SOURCE_DIRECTORY'] = project.shell_test.testSuite.executable
		//project.shell_test.environmentVariables['MDU_SHELLTEST_TESTUNIT_TEST_RESOURCE_DIRECTORY'] = project.shell_test.testSuite.executable
	}

	void apply(Project project) {

		addProjectExtensions(project)

		File toolResourcesDir = BundledResourcesPlugin.unpackBundledResources(project, ID, "tool")


		def extension = project.extensions.create(SHELL_TEST_EXTENSION_NAME, ShellTestPluginExtension, project)
		extension.with {
			returnCode {
				executionError = 23
				assertionFailure = 24
			}
			resultsDir = new File(project.buildDir, "test-results")
			throwErrorOnBadResult = true
			testSuite {
				shunit2Home = new File(toolResourcesDir, "shunit2-2.0.3")
				executable = new File(project.shell_test.testSuite.shunit2Home, "shunit2")
				runnerInclude = new File(toolResourcesDir, "test_runner.sh")
			}
			naming {
				removeCommonPrefix = true
				prefix = "test_"
				removeSuffix = false
			}
			check {
				enabled = canCheckScript()
				naming {
					prefix = "check_"
					removeSuffix = false
				}
				resultsDir = new File(project.buildDir, "check-results")
				throwErrorOnBadResult = false
			}
		}

		project.ext.ShellTestTask = ShellTestTask
		project.ext.AllShellTestsTask = CheckTestsResultsTask

		project.afterEvaluate {
			prepareEnvironment(project)
			addTasks(project)
			project
		}


	}

	private void addTasks(Project project) {
		if (project.shell_test.returnCode.executionError == 0 || project.shell_test.returnCode.assertionFailure == 0) throw new ShellTestException("returnCode.executionError and returnCode.executionError cannot be '0' ( since 0 is the success return code )")

		String projectStart = "${project.projectDir}/"

		String trimmedTestScriptsStart = projectStart
		if (project.shell_test.naming.removeCommonPrefix ) {
			trimmedTestScriptsStart = getBiggestPrefix(projectStart, project.shell_test.testScripts)
		}

		setupTestTasks(project, trimmedTestScriptsStart)

		String trimmedScriptsStart = projectStart
		if (project.shell_test.naming.removeCommonPrefix ) {
			trimmedScriptsStart = getBiggestPrefix(projectStart, project.shell_test.scripts)
		}

		if (project.shell_test.check.enabled) {

			if (!canCheckScript()) throw new GradleException("Cannot check script : no available command")

			setupCheckTasks(project, trimmedScriptsStart)
		}
	}

	private boolean canCheckScript() {
		return existsInPath("shellcheck")
	}

	private void setupTestTasks(Project project, String trimmedTestScriptsStart) {

		def resultsCheckTask = project.task(ALL_TESTS_RESULT_TASK_NAME, type:CheckTestsResultsTask) { }
		def allTestsTask = project.task(ALL_TESTS_TASK_NAME) { }

		if (project.shell_test.testScripts == null || project.shell_test.testScripts.size()==0) {
			project.getLogger().warn(LOGGING_PREFIX+"No test script found")
			return
		}

		project.shell_test.testScripts.each() { file ->

			String projectTrimmedName = file.absolutePath.startsWith(trimmedTestScriptsStart) ? file.absolutePath.substring(trimmedTestScriptsStart.size()) : file.absolutePath
			if (project.shell_test.naming.removeSuffix)
				projectTrimmedName = projectTrimmedName - FILENAME_SUFFIX_REGEX
			String simple_testname = projectTrimmedName

			String testname = projectTrimmedName.replaceAll("/",";")
			if (project.shell_test.naming.prefix)
				testname = project.shell_test.naming.prefix + testname

			def testTask = project.task(testname, type: ShellTestTask) {
				testName = simple_testname
				script = file
				if (project.shell_test.workingDir != null) workingDir = project.shell_test.workingDir
			}
			allTestsTask.dependsOn testTask
			testTask.finalizedBy resultsCheckTask
		}

		project.tasks[GRADLE_LIFECYCLE_TEST_TASK].dependsOn resultsCheckTask

	}

	private void setupCheckTasks(Project project, String trimmedScriptsStart) {

		def resultsCheckCheckTask = project.task(ALL_CHECKS_RESULT_TASK_NAME, type:CheckChecksResultsTask) { }
		def allChecksTask = project.task(ALL_CHECKS_TASK_NAME) { }

		if (project.shell_test.scripts == null || project.shell_test.scripts.size()==0) {
			project.getLogger().warn(LOGGING_PREFIX+"No script found for checking")
			return
		}

		project.shell_test.scripts.each() { file ->

			String projectTrimmedName = file.absolutePath.startsWith(trimmedScriptsStart) ? file.absolutePath.substring(trimmedScriptsStart.size()) : file.absolutePath
			if (project.shell_test.check.naming.removeSuffix)
				projectTrimmedName = projectTrimmedName - FILENAME_SUFFIX_REGEX
			String simple_testname = projectTrimmedName

			String checkname = projectTrimmedName.replaceAll("/",";")
			if (project.shell_test.check.naming.prefix)
				checkname = project.shell_test.check.naming.prefix + checkname

			def checkTask = project.task(checkname, type:ShellCheckTask) {
				testName = simple_testname
				script = file
				if (project.shell_test.workingDir != null) workingDir = project.shell_test.workingDir
			}

			allChecksTask.dependsOn checkTask
			checkTask.finalizedBy resultsCheckCheckTask
		}

	}


	private String getBiggestPrefix(String initialPrefix, FileCollection files) {
		String trimmedStart = initialPrefix
		int greatestCommonPrefixLength = 0

		if (files != null && files.size()>1) {
			String greatestCommonPrefix = null
			files.find() { file ->
				String filename = file.absolutePath
				if (! filename.startsWith(trimmedStart) ) {
					greatestCommonPrefixLength = 0
					return true
				}
				filename = filename.substring(trimmedStart.size())
				if (greatestCommonPrefix == null) {
					greatestCommonPrefix = filename
					greatestCommonPrefixLength = greatestCommonPrefix.length()
				} else {
					int length = stringsGreatestCommonPrefixLength (greatestCommonPrefix, filename)
					if (length < greatestCommonPrefixLength) {
						greatestCommonPrefixLength = length
						greatestCommonPrefix = filename.substring(0, length)
					}
				}
				return false
			}
			if (greatestCommonPrefixLength>0)
				trimmedStart = initialPrefix + greatestCommonPrefix
		} else {
			trimmedStart = initialPrefix
		}

		return trimmedStart
	}
}
