package net.tetrakoopa.gradle.plugin.shell.test

import net.tetrakoopa.gradle.plugin.shell.common.AbstractShellProjectPlugin
import net.tetrakoopa.gradle.plugin.shell.test.task.CheckTestsTesultsTask
import net.tetrakoopa.gradle.plugin.shell.test.task.ShellTestTask
import org.gradle.api.Plugin
import org.gradle.api.Project

import static ShellTestPluginExtension.SHELL_TEST_EXTENSION_NAME

class ShellTestPlugin extends AbstractShellProjectPlugin implements Plugin<Project> {

	public static final String ID = "net.tetrakoopa.shell-test"

	public static final String ALL_TESTS_RESULT_TASK_NAME = "shell-checktests"
	public static final String ALL_ALL_TESTS_TASK_NAME = "shell-test"

	public static final String ENVVAR_TEST_RESULTS_DIRECTORY = "MDU_SHELLTEST_TEST_RESULTS_DIRECTORY"

	private void preprareEnvironment(Project project) {
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
		}

		project.ext.ShellTestTask = ShellTestTask
		project.ext.AllShellTestsTask = CheckTestsTesultsTask

		preprareEnvironment(project)

		project.afterEvaluate {

			if (project.shell_test.returnCode.executionError == 0 || project.shell_test.returnCode.assertionFailure == 0) throw new ShellTestException("returnCode.executionError and returnCode.executionError cannot be '0' ( since 0 is the success return code )")

			def resultsCheckTask = project.task(ALL_TESTS_RESULT_TASK_NAME, type:CheckTestsTesultsTask) { }
			def allTestsTask = project.task(ALL_ALL_TESTS_TASK_NAME) { }

			project.shell_test.testScripts.each() { file ->

				def trimmedStart = "${project.projectDir}/"
				def testname = file.absolutePath.startsWith(trimmedStart) ? file.absolutePath.substring(trimmedStart.size()) : file.absolutePath

				def testTask = project.task("test_${testname}", type:ShellTestTask) {
					testName = testname
					script = file
					if (project.shell_test.workingDir != null) workingDir = project.shell_test.workingDir
				}
				allTestsTask.dependsOn testTask
				testTask.finalizedBy resultsCheckTask
			}
		}

	}
}

