package net.tetrakoopa.gradle.plugin.shell.test

import org.gradle.api.Project

class DefaultConfiguration {

	static void setDefaultPreConfig(ShellTestPluginExtension extension, Project project, File toolResourcesDir) {
		extension.with {
			returnCode {
				executionError = 23
				assertionFailure = 24
			}
			outputDirectory = new File(project.buildDir, "test-results")

			testSuite {
				shunit2Home = new File(toolResourcesDir, "shunit2")
				executable = new File(project.shell_test.testSuite.shunit2Home, "shunit2")
				runnerInclude = new File(toolResourcesDir, "test_runner.sh")
			}
			naming {
				removeCommonPrefix = true
				prefix = "test_"
				removeSuffix = false
			}
		}
	}

	static void setDefaultConfig(ShellTestPluginExtension extension, Project project) {
		if (extension.check != null) {

			if (extension.output.logDirectory == null)
				extension.output.logDirectory = new File(extension.outputDirectory, 'log')
			if (extension.output.generatedDirectory == null)
				extension.output.generatedDirectory = new File(extension.outputDirectory, 'generated')

			final ShellTestPluginExtension.Check check = extension.check
			if (check.naming.prefix == null) check.naming.prefix = "check_"
			if (check.resultsDir == null) check.resultsDir = new File(project.buildDir, "check")
		}
	}

}
