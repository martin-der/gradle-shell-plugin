package net.tetrakoopa.gradle.plugin.usual;

import org.gradle.api.file.RelativePath;

public interface UseFileDetails {

	void exclude();
	boolean isExcluded();

	RelativePath getRelativeOriginalPath();
	String getOriginalName();

	RelativePath getRelativeSourcePath();
	String getSourceName();

	void setRelativePath(RelativePath relativePath);
	RelativePath getRelativePath();
}
