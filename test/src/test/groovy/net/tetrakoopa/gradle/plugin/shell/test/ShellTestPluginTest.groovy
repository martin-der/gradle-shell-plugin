package net.tetrakoopa.gradle.plugin.shell.test

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

class ShellTestPluginTest extends Specification {

	@Shared
	Project project
	@Shared
	def projectDir

	def setup() {
		URL resource = getClass().getResource('/gradle/tested-project/build.gradle')
		projectDir = new File(resource.toURI()).getParentFile()
		project = ProjectBuilder.builder().withName('project').withProjectDir(projectDir).build()
	}

	def "no ShellTestPluginExtension is registered by default"() {
		expect:
		!project.extensions.findByType(ShellTestPluginExtension)
	}

	def "ShellTestPluginExtension is registered on project evaluation"() {
		when: "plugin applied to project"
		project.evaluate()
		then:
		project.extensions.findByType(ShellTestPluginExtension)
	}

	def "ShellTestPluginExtension is registered as 'shell_test'"() {
		when: "plugin applied to project"
		project.evaluate()
		assert ShellTestPluginExtension.SHELL_TEST_EXTENSION_NAME == 'shell_test'
		then:
		project.extensions.findByName('shell_test') in ShellTestPluginExtension
	}
}
