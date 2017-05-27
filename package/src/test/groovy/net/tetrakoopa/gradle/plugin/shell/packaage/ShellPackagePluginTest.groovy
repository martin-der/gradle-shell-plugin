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

	/*def "no Document  tasks are registered by default"() {
		expect:
		!project.tasks.withType(BuildDebianPackageTask)
	}

	def "BuildDebianPackage tasks are registered on project evaluation"() {
		when: "plugin applied to project"
		project.evaluate()
		then: "there is a BuildDebianPackage task registered"
		project.tasks.withType(BuildDebianPackageTask)
	}

	def "BuildDebianPackage task is registered as 'buildDeb'"() {
		when: "plugin applied to project"
		project.evaluate()
		assert DEBPKGTASK_NAME == 'buildDeb'
		then: "there is a 'buildDeb' task registered"
		project.tasks.findByName('buildDeb') in BuildDebianPackageTask
	}

	def "can handle a debian configuration"() {
		when: "project example project 'projectname' is evaluated"
		Project project = ProjectBuilder.builder().withName('projectname').withProjectDir(projectDir).build()
		project.evaluate()

		then: "extension properties are mapped to task properties"
		Task buildDebTask = project.tasks.findByName(DEBPKGTASK_NAME)
		buildDebTask != null
		buildDebTask.packagename == "packagename"
		buildDebTask.changelogFile == new File("${projectDir}/../packagename/debian/changelog").canonicalFile
		buildDebTask.controlDirectory == new File("${projectDir}/../packagename/control").canonicalFile
		buildDebTask.publications == ['mavenStuff']
		buildDebTask.data in Data
		buildDebTask.outputFile == new File("${projectDir}/build/packagename-${project.version}.deb").canonicalFile
	}

	def "buildDeb is dependent on publicationTask"() {
		when: "project example project 'projectname' is evaluated"
		Project project = ProjectBuilder.builder().withName('projectname').withProjectDir(projectDir).build()
		project.evaluate()
		then:
		Task buildDebTask = project.tasks.findByName(DEBPKGTASK_NAME)
		Task publicationTask = project.tasks.findByName(PUBLISH_LOCAL_LIFECYCLE_TASK_NAME)
		buildDebTask.taskDependencies.getDependencies(buildDebTask).contains(publicationTask)
	}

	def "buildDeb is dependent on assemble task"() {
		when: "project example project 'projectname' is evaluated"
		Project project = ProjectBuilder.builder().withName('projectname').withProjectDir(projectDir).build()
		project.evaluate()
		then:
		Task buildDebTask = project.tasks.findByName(DEBPKGTASK_NAME)
		buildDebTask.taskDependencies.getDependencies(buildDebTask).contains(project.tasks.findByName(ASSEMBLE_TASK_NAME))
	}*/
}
