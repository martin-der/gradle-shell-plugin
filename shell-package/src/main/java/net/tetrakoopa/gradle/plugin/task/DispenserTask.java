package net.tetrakoopa.gradle.plugin.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import lombok.Cleanup;

import net.tetrakoopa.gradle.SystemUtil;
import net.tetrakoopa.gradle.plugin.shell.FileModeRegistry;
import net.tetrakoopa.gradle.plugin.shell.ResourceUtil;
import net.tetrakoopa.gradle.plugin.shell.ShellPackageDispenserArchiveBuilder;
import net.tetrakoopa.gradle.plugin.shell.ShellPackageDispenserExecutorBuilder;
import net.tetrakoopa.gradle.plugin.shell.ShellPackagePlugin;
import net.tetrakoopa.gradle.plugin.shell.ShellPluginExtension;


public abstract class DispenserTask extends DefaultTask {

	@Input
	public abstract Property<String> getProjectName();

	@Input @Optional
	public abstract Property<String> getProjectVersion();

	@Input
	public abstract Property<String> getProjectLabel();

	@Input @Optional
	public abstract Property<String> getDistributionName();

    @InputFile @Optional
	public abstract RegularFileProperty getBanner();

    @InputFile @Optional
	public abstract RegularFileProperty getReadme();

    @Input
	public abstract Property<ShellPluginExtension.MultiActionModeStrategy> getMultiActionModeStrategy();

	/**
	 * Action the generated package falls back to when its first parameter names none, empty when
	 * the build script asked for no default.
	 */
	@Input @Optional
	public abstract Property<String> getDefaultAction();

	@InputFiles
	public abstract ConfigurableFileCollection getSources();

	/**
	 * Directory the {@code source} copy task fills. Only used to report an actionable error when it
	 * ends up empty, which cannot be detected any earlier.
	 */
	@Internal
	public abstract DirectoryProperty getContentDirectory();

	@Input @Optional
	public abstract Property<String> getLauncherReactorScript();

	@Input @Optional
	public abstract Property<Boolean> getLauncherReactorEnvironment();

	@Input
	public abstract Property<Boolean> getMakeExecutable();

	@OutputFile
	public abstract RegularFileProperty getExecutorTarget();

    @OutputFile
	public abstract RegularFileProperty getTarget();

	@Input @Optional
	public abstract Property<Boolean> getUsePersistentTemporaryDirectory();

	private static final String DISPENSE_FILENAME = "dispense.sh";

	@Inject
	public DispenserTask() {
		getProjectName().convention(getProject().getName());
		getProjectLabel().convention(getProjectName());
		getMultiActionModeStrategy().convention(ShellPluginExtension.MultiActionModeStrategy.ACTION_MODE_PREFIX);
		getMakeExecutable().convention(true);
		getExecutorTarget().convention(getProject().getLayout().getBuildDirectory().file(ShellPackagePlugin.EXPLODED_WORK_PATH+File.separator+DISPENSE_FILENAME));
		getTarget().convention(() 
            -> getProject().getLayout().getBuildDirectory().file(ShellPackagePlugin.PLUGIN_WORK_FOLDER+"/"+ShellPackagePlugin.DISPENSER_WORK_FOLDER+"/"+buildArchiveFileName()+".sh")
            .get().getAsFile());
	}

	@TaskAction
	public void execute() {
        final File dispenserFile = getExecutorTarget().get().getAsFile();
        
        if (!dispenserFile.getParentFile().exists()) {
            dispenserFile.getParentFile().mkdirs();
        }

        final File explodedWorkFile = getProject().getLayout().getBuildDirectory().file(ShellPackagePlugin.EXPLODED_WORK_PATH).get().getAsFile();
        final File contentExplodedDirectory = new File(explodedWorkFile, "content");

        // On Windows the filesystem cannot store Unix permission bits, so the modes of the files the
        // plugin generates itself are recorded here and re-applied when building the archive. On a
        // real POSIX system the modes stay on the files and are simply read back from there.
        final Map<String, Integer> recordedModes = new LinkedHashMap<String, Integer>();

        requireNonEmptyContent(contentExplodedDirectory);

        try {
            copyScriptUtils(explodedWorkFile);
        } catch (IOException e) {
            throw new InvalidUserDataException("Failed to copy runtime util scripts : "+e.getMessage(), e);
        }

        if (getLauncherReactorScript().isPresent()) {
            final String reactorPath = getLauncherReactorScript().get();
            final File reactor = new File(contentExplodedDirectory, reactorPath);
            if (! reactor.isFile()) {
                throw new InvalidUserDataException(
                    "In shell_package > launcher > script : '"+reactorPath+"' is not a file in the packaged "
                    + "content. Check that it exists and that your 'source' block actually packages it"
                    + (reactorPath.contains("\\") ? " (note the Windows path separator: use '/')" : "")
                    + ". Searched in : "+reactor.getParent());
            }
            try {
                SystemUtil.makeExecutable(reactor, false, false);
                if (SystemUtil.isWindows()) {
                    recordedModes.put("content/"+reactorPath.replace('\\', '/'), SystemUtil.executableMode(reactor, false, false));
                }
            } catch (IOException e) {
                throw new InvalidUserDataException("Failed to make '"+reactor.getAbsolutePath()+"' executable : "+e.getMessage(), e);
            }
        }

        try (ShellPackageDispenserExecutorBuilder builder = new ShellPackageDispenserExecutorBuilder(dispenserFile)) {
            builder
                .packageName(getProjectName().get())
                .label(getProjectLabel().get())
                .packageVersion(getProjectVersion().getOrNull())
                .actionModeStrategy(getMultiActionModeStrategy().get())
                .defaultAction(getDefaultAction().getOrNull())
                .showBanner(getBanner().isPresent())
                .showReadme(getReadme().isPresent())
                .launcherScript(getLauncherReactorScript().getOrNull())
                .launcherScriptHasEnvironmentProperties(getLauncherReactorEnvironment().getOrElse(false));
            builder.build();
            if (SystemUtil.isWindows()) {
                recordedModes.put(DISPENSE_FILENAME, SystemUtil.executableMode(dispenserFile, true, false));
            }
        } catch (IOException e) {
            throw new InvalidUserDataException("Failed to create dispense script : "+e.getMessage(), e);
        }

        if (SystemUtil.isWindows()) {
            try {
                FileModeRegistry.write(FileModeRegistry.registryFileFor(explodedWorkFile), recordedModes);
            } catch (IOException e) {
                throw new InvalidUserDataException("Failed to record modes in '"+FileModeRegistry.registryFileFor(explodedWorkFile).getAbsolutePath()+"' : "+e.getMessage(), e);
            }
        }

        final File archiveFile = getTarget().get().getAsFile();
        try (ShellPackageDispenserArchiveBuilder builder = new ShellPackageDispenserArchiveBuilder(explodedWorkFile, archiveFile)) {
            builder.makeExecutable(getMakeExecutable().getOrElse(true));
            builder.applicationName(getProjectName().get());
            builder.applicationVersion(getProjectVersion().getOrNull());
            builder.usePersistentTempFolder(getUsePersistentTemporaryDirectory().getOrElse(true));
            builder.build();
    		getLogger().lifecycle("Created archive file '{}'", archiveFile.getAbsolutePath());
        } catch (IOException e) {
            throw new InvalidUserDataException("Failed to create archive : "+e.getMessage(), e);
        }


	}

    /**
     * Refuses to build a package that contains nothing.
     *
     * <p>Gradle's {@code Copy} task treats a {@code from} path that does not exist, or an
     * {@code include} pattern that matches nothing, as a successful no-op. The archive is then
     * produced with no {@code content} directory at all, and the only symptom the user ever sees is
     * a "No file to install" message from the generated installer, long after the Gradle build that
     * supposedly succeeded. Failing here keeps the error next to the configuration that caused it.
     */
    private void requireNonEmptyContent(File contentDirectory) {
        final File[] entries = contentDirectory.listFiles();
        if (entries != null && entries.length > 0) {
            return;
        }
        throw new InvalidUserDataException(
            "In shell_package > source : no file was selected for packaging, so the generated archive would "
            + "be empty and unable to install anything.\n"
            + "  Check that every 'from' path exists and that your 'include'/'exclude' patterns match at "
            + "least one file.\n"
            + "  Content directory searched : " + contentDirectory);
    }

    public void target(Provider<File> provider) {
        getExecutorTarget().fileProvider(provider);
    }
    public void target(File target) {
        getExecutorTarget().fileValue(target);
    }


    private String buildArchiveFileName() {
        final StringBuilder builder = new StringBuilder();
        // 'distributionName' is precisely "the name of the artifact I distribute", so it wins over
        // 'name' when present; previously it was assigned to a field nothing ever read.
        final String archiveBaseName = getDistributionName().getOrElse(getProjectName().get());
        builder.append(archiveBaseName);
        if (getProjectVersion().isPresent()) {
            builder
                .append("-")
                .append(getProjectVersion().get());
        }
        return builder.toString();
    }

    private void copyScriptUtils(File explodedDir) throws IOException {
        final File scriptUtilsDir = new File(explodedDir, "util");
        Files.createDirectories(scriptUtilsDir.toPath());
        for (String resourceFileName : List.of("log.sh", "flowui-builder-json.sh", "flowui-dumbcli.sh", "flowui-humbletui.sh", "flowui.sh", "shell-util.sh")) {
            final String resourcePath = "/runtime/"+resourceFileName;
            try {
                @Cleanup
                final var inputStream = ResourceUtil.getClassPathResource(resourcePath);

                @Cleanup
                final var output = new FileOutputStream(new File(scriptUtilsDir, resourceFileName));

                inputStream.transferTo(output);
            } catch (Exception e) {
                throw new IOException("Failed to copy '"+resourcePath+"'", e);
            }
        }
    }


 
}
