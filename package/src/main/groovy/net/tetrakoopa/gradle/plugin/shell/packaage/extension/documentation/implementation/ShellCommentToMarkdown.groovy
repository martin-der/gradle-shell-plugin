package net.tetrakoopa.gradle.plugin.shell.packaage.extension.documentation.implementation

import net.tetrakoopa.gradle.plugin.common.exception.PluginException
import net.tetrakoopa.gradle.plugin.shell.packaage.ShellPackagePlugin
import net.tetrakoopa.gradle.plugin.shell.packaage.extension.documentation.Lot
import net.tetrakoopa.poignee.bundledresources.BundledResourcesPlugin
import org.gradle.api.Project

class ShellCommentToMarkdown implements Lot.ToDocumentationConverter {

	private File converterScript

	@Override
	void construct(Project project) {
		File toolDir = BundledResourcesPlugin.unpackBundledResources(project, ShellPackagePlugin.ID, "tool")
		this.converterScript = new File(toolDir,"/doc/shdoc/shdoc_io_w.sh")
	}

	@Override
	void convert(Project project, String originalName, File source, File destination) {
		def errOs = new ByteArrayOutputStream()
		try {
			project.exec {
				commandLine converterScript.absolutePath, "${source.absolutePath}", "${destination.absolutePath}"
				errorOutput = errOs
			}
		} catch (Exception exception) {
			throw new PluginException('Failed to convert comment to markdown :\n'+errOs.toString(), exception);
		}
	}

	@Override
	void destroy(Project project) {

	}
}
