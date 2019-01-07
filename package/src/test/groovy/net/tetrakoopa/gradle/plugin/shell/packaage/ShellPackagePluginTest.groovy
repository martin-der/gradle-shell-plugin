package net.tetrakoopa.gradle.plugin.shell.packaage

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files

class ShellPackagePluginTest extends Specification {

	@Shared
	Project project
	@Shared
	def projectDir
	@Shared
	def buildDir

	def setup() {
		URL resource = getClass().getResource('/gradle/packaged-project/build.gradle')
		projectDir = new File(resource.toURI()).getParentFile()
		project = ProjectBuilder.builder().withName('project').withProjectDir(projectDir).build()
		buildDir = Files.createTempDirectory("Project.buildDir")
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

	def "no packaging tasks are registered by default"() {
		expect:
		!project.tasks.findByName(ShellPackagePlugin.TASK_NAME_INSTALLER)
		!project.tasks.findByName(ShellPackagePlugin.TASK_NAME_DOCUMENTATION)
		!project.tasks.findByName(ShellPackagePlugin.TASK_NAME_PACKAGE_ZIP)
	}

	def "packaging tasks are registered on project evaluation"() {
		when: "plugin applied to project"
		project.evaluate()
		then: "there are a packaging tasks registered"
		project.tasks.findByName(ShellPackagePlugin.TASK_NAME_INSTALLER)
		project.tasks.findByName(ShellPackagePlugin.TASK_NAME_DOCUMENTATION)
		project.tasks.findByName(ShellPackagePlugin.TASK_NAME_PACKAGE_ZIP)
	}

//	def "packaging tasks  registered as '...'"() {
//		when: "plugin applied to project"
//		project.evaluate()
//		assert SOME_TASK_NAME == 'documentation'
//		then: "there is a 'documentation' task registered"
//		project.tasks.findByName('documentation') in DocumentationTask
//	}

	/*def "can handle a shell-package configuration"() {
		when: "project example project 'projectname' is evaluated"
		Project project = ProjectBuilder.builder().withName('projectname').withProjectDir(projectDir).build()
		project.evaluate()

		then: "extension properties are mapped to task properties"
		Task packageZip = project.tasks.findByName(ShellPackagePlugin.TASK_NAME_PACKAGE_ZIP)
		packageZip != null
		packageZip.packagename == "packagename"
		packageZip.changelogFile == new File("${projectDir}/../packagename/debian/changelog").canonicalFile
		packageZip.controlDirectory == new File("${projectDir}/../packagename/control").canonicalFile
		packageZip.data in Data
		packageZip.outputFile == new File("${project.buildDir}/packagename-${project.version}.deb").canonicalFile
	}*/

	def "packageZip is dependent on documentation Task"() {
		when: "project example project is evaluated"
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.evaluate()
		then:
		Task packageZipTask = project.tasks.findByName(ShellPackagePlugin.TASK_NAME_PACKAGE_ZIP)
		Task documentationTask = project.tasks.findByName(ShellPackagePlugin.TASK_NAME_DOCUMENTATION)
		packageZipTask.taskDependencies.getDependencies(packageZipTask).contains(documentationTask)
	}

	def "installer is dependent on packageZip Task"() {
		when: "project example project is evaluated"
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.evaluate()
		then:
		Task installerTask = project.tasks.findByName(ShellPackagePlugin.TASK_NAME_INSTALLER)
		Task packageZipTask = project.tasks.findByName(ShellPackagePlugin.TASK_NAME_PACKAGE_ZIP)
		def dependencies = installerTask.taskDependencies.getDependencies(installerTask)
		dependencies.contains(packageZipTask)
	}

	/*def "documentation create a directory with two files"() {
		when: "project task documentation is invoked"
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).with({
			project.buildDir = buildDir
		}).build()
		project.evaluate()
		then:
		println "Temp dir : "+((File)buildDir).getAbsolutePath()

	}*/
}
