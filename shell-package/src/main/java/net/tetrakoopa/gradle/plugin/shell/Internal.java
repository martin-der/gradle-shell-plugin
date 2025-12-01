package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.tasks.Copy;

import net.tetrakoopa.gradle.plugin.task.DispenserTask;
import net.tetrakoopa.gradle.plugin.task.TextFileSourceTask;

public class Internal {

	String name;
	Date buildDate;
	File toolResourcesDir;
	File workingDir;
	File dispenserWorkingDir;
	File explodedPackageDir;
	File contentDir;
	File resourceDir;
	File installerFilesRootDir;
	ConfigurableFileCollection intermediateSources;
	final List<FileCopyDetails> sourceDetails = new ArrayList<>();
	final Map<String, String> descriptionValues = new HashMap<>();

	public static class BaseTask {
		Copy prepareSources;
		TextFileSourceTask prepareBanner;
		DispenserTask dispenserTask;
	}

	final BaseTask task = new BaseTask(); 
}
