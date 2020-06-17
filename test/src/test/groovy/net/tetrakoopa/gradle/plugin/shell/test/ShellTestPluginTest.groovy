package net.tetrakoopa.gradle.plugin.shell.test

import net.tetrakoopa.poignee.bundledresources.BundledResourcesPlugin
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

	@Shared
	String testedProjectBuildDir
	@Shared
	String bundledZipAbsolutePath

	def setupSpec() {

//		testedProjectBuildDir = System.getenv('net.tetrakoopa.bundled-resources.project.buildDir')
//		if (testedProjectBuildDir == null) throw new IllegalStateException()
//
//		bundledZipAbsolutePath = "${testedProjectBuildDir}/net.tetrakoopa.bundled-resources/${ShellTestPlugin.ID}.tool.zip"
	}

	def setup() {
		URL resource = getClass().getResource('/gradle/tested-project/build.gradle')
		projectDir = new File(resource.toURI()).getParentFile()
		project = ProjectBuilder.builder().withName('project').withProjectDir(projectDir).build()
		fakeAllBundledResources(project)
	}

	private void prepareAllBundledResources(Project project) {
		prepareBundledResources(project, "tool")
	}
	private void prepareBundledResources(Project project, String name) {
		BundledResourcesPlugin.unpackBundledResourcesUsingThisZip(project, ShellTestPlugin.ID, name, new File(bundledZipAbsolutePath))
	}
	private void fakeAllBundledResources(Project project) {
		fakeBundledResources(project, "tool")
	}
	private void fakeBundledResources(Project project, String name) {
		// Assume this project is the top project
		File dir = project.file("${project.buildDir}/${ShellTestPlugin.ID}/${name}")
		dir.mkdirs()
		new File(dir, ".unpack-ok").write("ok")
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

	/*def "no BuildDebianPackage tasks are registered by default"() {
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
