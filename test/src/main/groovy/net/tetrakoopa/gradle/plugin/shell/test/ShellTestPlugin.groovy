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

	private void preprareEnvironment(Project project) {
		project.shell_test.environmentVariables['POIGNEE_TESTUNIT_SHUNIT2_EXEC'] = project.shell_test.testSuite.executable
		project.shell_test.environmentVariables['POIGNEE_TESTUNIT_PROJECT_DIRECTORY'] = project.projectDir
		//project.shell_test.environmentVariables['POIGNEE_TESTUNIT_SOURCE_DIRECTORY'] = project.shell_test.testSuite.executable
		//project.shell_test.environmentVariables['POIGNEE_TESTUNIT_TEST_SOURCE_DIRECTORY'] = project.shell_test.testSuite.executable
		//project.shell_test.environmentVariables['POIGNEE_TESTUNIT_TEST_RESOURCE_DIRECTORY'] = project.shell_test.testSuite.executable
	}

	void apply(Project project) {
		File toolResourcesDir = prepareResources(project, ID, "tool")

		def extension = project.extensions.create(SHELL_TEST_EXTENSION_NAME, ShellTestPluginExtension, project)
		extension.with {
			assertionFailureErrorCode = 5
			testSuite {
				shunit2Home = new File(toolResourcesDir, "shunit2-2.0.3")
				executable = new File(project.shell_test.testSuite.shunit2Home, "shunit2")
			}
		}

		project.ext.ShellTestTask = ShellTestTask
		project.ext.AllShellTestsTask = CheckTestsTesultsTask

		preprareEnvironment(project)

		project.afterEvaluate {

			def resultsCheckTask = project.task(ALL_TESTS_RESULT_TASK_NAME, type:CheckTestsTesultsTask) { }
			def allTestsTask = project.task(ALL_ALL_TESTS_TASK_NAME) { }

			project.shell_test.testScripts.each() { file ->

				def trimmedStart = "${project.projectDir}/"
				def filename = file.absolutePath.startsWith(trimmedStart) ? file.absolutePath.substring(trimmedStart.size()) : file.absolutePath

				def testTask = project.task("test_${filename}", type:ShellTestTask) {
					script = file
					if (project.shell_test.workingDir != null) workingDir = project.shell_test.workingDir
				}
				allTestsTask.dependsOn testTask
				testTask.finalizedBy resultsCheckTask
			}
		}

	}
}

