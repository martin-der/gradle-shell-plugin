package net.tetrakoopa.gradle.plugin.shell.test

import net.tetrakoopa.gradle.plugin.shell.common.AbstractShellProjectPlugin
import net.tetrakoopa.gradle.plugin.shell.test.task.CheckTestsTesultsTask
import net.tetrakoopa.gradle.plugin.shell.test.task.ShellTestTask
import org.gradle.api.Plugin
import org.gradle.api.Project

import static ShellTestPluginExtension.SHELL_TEST_EXTENSION_NAME

class ShellTestPlugin extends AbstractShellProjectPlugin implements Plugin<Project> {

	public static final String ID = "net.tetrakoopa.shell-test"

	public static final String LOGGING_PREFIX = "Shell-Test : "

	public static final String ALL_TESTS_RESULT_TASK_NAME = "shell-checktests"
	public static final String ALL_ALL_TESTS_TASK_NAME = "shell-test"

	public static final String ENVVAR_TEST_RESULTS_DIRECTORY = "MDU_SHELLTEST_TEST_RESULTS_DIRECTORY"

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
		File toolResourcesDir = prepareResources(project, ID, "tool")

		def extension = project.extensions.create(SHELL_TEST_EXTENSION_NAME, ShellTestPluginExtension, project)
		extension.with {
			returnCode {
				executionError = 23
				assertionFailure = 24
			}
			resultsDir = new File(project.buildDir, "test-results")
			testSuite {
				shunit2Home = new File(toolResourcesDir, "shunit2-2.0.3")
				executable = new File(project.shell_test.testSuite.shunit2Home, "shunit2")
				runnerInclude = new File(toolResourcesDir, "test_runner.sh")
			}
			naming {
				removeCommonPrefix = true
				prefix = "test_"
			}
		}

		project.ext.ShellTestTask = ShellTestTask
		project.ext.AllShellTestsTask = CheckTestsTesultsTask

		prepareEnvironment(project)

		project.afterEvaluate {
			addTasks(project)
		}


	}

	private void addTasks(Project project) {
		if (project.shell_test.returnCode.executionError == 0 || project.shell_test.returnCode.assertionFailure == 0) throw new ShellTestException("returnCode.executionError and returnCode.executionError cannot be '0' ( since 0 is the success return code )")

		def resultsCheckTask = project.task(ALL_TESTS_RESULT_TASK_NAME, type:CheckTestsTesultsTask) { }
		def allTestsTask = project.task(ALL_ALL_TESTS_TASK_NAME) { }

		if (project.shell_test.testScripts == null || project.shell_test.testScripts.size()==0) {
			project.getLogger().warn(LOGGING_PREFIX+"No test script found")
			return
		}

		String projectStart = "${project.projectDir}/"
		String trimmedStart = projectStart
		int greatestCommonPrefixLength = 0

		if (project.shell_test.naming.removeCommonPrefix && project.shell_test.testScripts.size()>1) {
			String greatestCommonPrefix = null
			project.shell_test.testScripts.find() { file ->
				String filename = file.absolutePath
				if (! filename.startsWith(projectStart) ) {
					greatestCommonPrefixLength = 0
					return true
				}
				filename = filename.substring(projectStart.size())
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
				trimmedStart = projectStart + greatestCommonPrefix
		} else {
			trimmedStart = projectStart
		}

		project.shell_test.testScripts.each() { file ->

			String testname = file.absolutePath.startsWith(trimmedStart) ? file.absolutePath.substring(trimmedStart.size()) : file.absolutePath

			if (project.shell_test.naming.prefix)
				testname = project.shell_test.naming.prefix + testname

			def testTask = project.task(testname, type:ShellTestTask) {
				testName = testname
				script = file
				if (project.shell_test.workingDir != null) workingDir = project.shell_test.workingDir
			}
			allTestsTask.dependsOn testTask
			testTask.finalizedBy resultsCheckTask
		}

	}
}

