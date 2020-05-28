package net.tetrakoopa.gradle.plugin.shell.packaage.helper

import net.tetrakoopa.gradle.plugin.shell.packaage.resource.DefaultUseFileDetails
import net.tetrakoopa.gradle.plugin.shell.packaage.resource.UseFileDetails
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath

class CommonPackageHelper {

	static UseFileDetails useFileDetails(FileCopyDetails fileCopyDetails, String into) {
		RelativePath newRelativePath
		if (into != null) {
			String[] newSegments = into.split('/')
			newRelativePath = new RelativePath(true, (String[]) (newSegments + fileCopyDetails.name))
		} else {
			newRelativePath = new RelativePath(true, fileCopyDetails.relativePath.segments)
		}
		RelativePath relativeIntermediatePath = new RelativePath(true, fileCopyDetails.relativePath.segments)
		return new DefaultUseFileDetails(fileCopyDetails.relativeSourcePath, relativeIntermediatePath, newRelativePath)
	}


}
