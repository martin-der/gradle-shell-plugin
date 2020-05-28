package net.tetrakoopa.gradle.plugin.shell.packaage.resource

import org.gradle.api.file.RelativePath

class DefaultUseFileDetails implements UseFileDetails {

	private boolean included = true

	boolean isExcluded() { return !included }

	private RelativePath relativeSourcePath
	private RelativePath relativeOriginalPath
	private RelativePath relativePath

	private DefaultUseFileDetails(RelativePath relativeOriginalPath, RelativePath relativeSourcePath, RelativePath relativePath) {
		if (relativeOriginalPath == null) relativeOriginalPath = relativeSourcePath
		this.relativeOriginalPath = new RelativePath(true, relativeOriginalPath.getSegments())
		this.relativeSourcePath = new RelativePath(true, relativeSourcePath.getSegments())
		this.relativePath = new RelativePath(true, relativePath.getSegments())
	}
	private DefaultUseFileDetails(RelativePath relativeSourcePath, RelativePath relativePath) {
		this(null, relativeSourcePath, relativePath)
	}

	@Override
	void exclude() {
		included = false
	}

	@Override
	RelativePath getRelativeOriginalPath() {
		return relativeOriginalPath
	}

	@Override
	String getOriginalName() {
		return relativeOriginalPath.getLastName()
	}

	@Override
	RelativePath getRelativeSourcePath() {
		return relativeSourcePath
	}

	@Override
	String getSourceName() {
		return relativeSourcePath.getLastName()
	}

	@Override
	void setRelativePath(RelativePath relativePath) {
		this.relativePath = relativePath
	}

	@Override
	RelativePath getRelativePath() {
		return relativePath
	}
}
