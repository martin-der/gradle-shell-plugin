package net.tetrakoopa.gradle.plugin.shell.packaage.helper.installer

import net.tetrakoopa.gradle.plugin.shell.packaage.ShellPackagePlugin
import net.tetrakoopa.gradle.plugin.shell.packaage.helper.CommonPackageHelper
import net.tetrakoopa.gradle.plugin.shell.packaage.resource.InstallSpec
import net.tetrakoopa.gradle.plugin.shell.packaage.resource.UseFileDetails
import net.tetrakoopa.gradle.plugin.shell.packaage.util.MultilinePropertyWriter
import org.gradle.api.Project
import org.gradle.api.file.FileCopyDetails

class InstallerHelper {

	static addScripts(ShellPackagePlugin.Internal internal, Project project) {
		project.copy {
			from new File(internal.toolResourcesDir, 'install/scripts')
			into new File(internal.installerFilesRootDir, 'script')
		}

	}

	static addComponents(List<FileCopyDetails> sourceDetails, List<InstallSpec> installSpecs, File installerComponentsDir) {
		installSpecs.each {
			installSpec ->
				File componentDir = new File(installerComponentsDir,installSpec.name)
				componentDir.mkdirs()

				MultilinePropertyWriter headerWriter = new MultilinePropertyWriter(new File(componentDir, 'header'), 'utf-8')
				headerWriter.appendProperty('importance', installSpec.importance.name())
				headerWriter.appendProperty('description', installSpec.description)
				headerWriter.close()

				//if (header.exists()) header.delete()
				MultilinePropertyWriter copyWriter = new MultilinePropertyWriter(new File(componentDir, 'copy'), 'utf-8')
				boolean first = true

				sourceDetails.forEach { FileCopyDetails fileCopyDetails ->
					final UseFileDetails details = CommonPackageHelper.useFileDetails(fileCopyDetails, null)

					if (first) first = false
					else copyWriter.append('\n')
					copyWriter.append('file\n')
					copyWriter.appendProperty('src', details.relativeSourcePath.toString())
					copyWriter.appendProperty('dest', details.relativePath.toString())

				}
				copyWriter.close()
		}
	}
}
