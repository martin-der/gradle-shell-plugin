package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCopyDetails;

// import lombok.Getter;
// import lombok.Setter;

// @Getter @Setter
public class Internal {
	String name;
	Date buildDate;
	File toolResourcesDir;
	File workingDir;
	File explodedPackageDir;
	File contentDir;
	File installerFilesRootDir;
	ConfigurableFileCollection intermediateSources;
	final List<FileCopyDetails> sourceDetails = new ArrayList<>();
	final Map<String, String> descriptionValues = new HashMap<>();
}
