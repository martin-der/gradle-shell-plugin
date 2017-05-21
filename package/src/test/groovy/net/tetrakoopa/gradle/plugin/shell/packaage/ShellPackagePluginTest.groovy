package net.tetrakoopa.gradle.plugin.shell.packaage

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

class ShellPackagePluginTest extends Specification {

	@Shared
	Project project
	@Shared
	def projectDir

	def setup() {
		URL resource = getClass().getResource('/gradle/packaged-project/build.gradle')
		projectDir = new File(resource.toURI()).getParentFile()
		project = ProjectBuilder.builder().withName('project').withProjectDir(projectDir).build()
	}

	def "no ShellPackagePluginExtension is registered by default"() {
		expect:
		!project.extensions.findByType(ShellPackagePluginExtension)
	}

	def "ShellPackagePluginExtension is registered on project evaluation"() {
		when: "plugin applied to project"
		project.evaluate()
		then:
		project.extensions.findByType(ShellPackagePluginExtension)
	}

	def "ShellPackagePluginExtension is registered as 'shell_package'"() {
		when: "plugin applied to project"
		project.evaluate()
		assert ShellPackagePluginExtension.SHELL_PACKAGE_EXTENSION_NAME == 'shell_package'
		then:
		project.extensions.findByName('shell_package') in ShellPackagePluginExtension
	}
}
