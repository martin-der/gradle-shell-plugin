package net.tetrakoopa.gradle.plugin.shell.packaage.resource

import org.gradle.api.file.RelativePath

interface UseFileDetails {

	void exclude()
	boolean isExcluded()

	RelativePath getRelativeOriginalPath()
	String getOriginalName()

	RelativePath getRelativeSourcePath()
	String getSourceName()

	void setRelativePath(RelativePath relativePath)
	RelativePath getRelativePath()
}