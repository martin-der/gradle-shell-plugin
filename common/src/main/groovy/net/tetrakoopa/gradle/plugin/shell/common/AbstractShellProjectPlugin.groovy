package net.tetrakoopa.gradle.plugin.shell.common

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import net.tetrakoopa.mdu4j.util.IOUtil
import org.gradle.tooling.model.build.GradleEnvironment

import net.tetrakoopa.mdu4j.util.SystemUtil

abstract class AbstractShellProjectPlugin implements Plugin<Project> {

	protected getTopProject(Project project) {
		while (project.getParent() != null) project = project.getParent()
		return project
	}

	protected File prepareResources(Project project, String pluginId, String name) {

		project.ext.getTopProject = { return getTopProject(project) }

		Project topProject = getTopProject(project)
		File resourcesDir = topProject.file("${topProject.buildDir}/${pluginId}/${name}")
		File resourcesZip = topProject.file("${topProject.buildDir}/${pluginId}/${name}.zip")

		File okFile = new File(resourcesDir, ".unpack-ok");
		if (okFile.exists()) {
			return resourcesDir
		}

		resourcesDir.mkdirs()
		String pluginBundledResource = "${pluginId}.${name}.zip"
		InputStream toolInput = getClass().getClassLoader().getResourceAsStream(pluginBundledResource)
		if (toolInput == null) throw new NullPointerException("No such resource '${pluginBundledResource}'")
		IOUtil.copy((InputStream)toolInput, new FileOutputStream(resourcesZip))

		topProject.copy {
			from topProject.zipTree(resourcesZip)
			into "${resourcesDir}"
		}
		resourcesZip.delete()

		okFile.append(new Date().toString().bytes)

		return resourcesDir
	}

	protected boolean existsInPath(String executable) {
		return SystemUtil.findExecutableInPath(executable) != null
	}

}