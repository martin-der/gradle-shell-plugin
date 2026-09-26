package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;

import org.gradle.api.tasks.Copy;

import net.tetrakoopa.gradle.plugin.task.DispenserTask;
import net.tetrakoopa.gradle.plugin.task.ShellPropertiesTask;
import net.tetrakoopa.gradle.plugin.task.TextFileSourceTask;

/**
 * Working directories and task handles shared between the plugin's wiring and its tasks.
 *
 * <p>Not part of the DSL: users configure {@link ShellPluginExtension}, never this.
 */
public class Internal {

	File workingDir;
	File dispenserWorkingDir;
	File explodedPackageDir;
	File contentDir;
	File resourceDir;

	public static class BaseTask {
		Copy prepareSources;
		TextFileSourceTask prepareBanner;
		TextFileSourceTask prepareReadme;
		ShellPropertiesTask prepareLauncherProperties;
		DispenserTask dispenserTask;
	}

	final BaseTask task = new BaseTask();
}
