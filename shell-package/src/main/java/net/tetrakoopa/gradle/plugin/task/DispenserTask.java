package net.tetrakoopa.gradle.plugin.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import lombok.Cleanup;
import net.tetrakoopa.gradle.SystemUtil;
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
	public abstract Property<String> getAuthor();

    @InputFile @Optional
	public abstract RegularFileProperty getBanner();

    @InputFile @Optional
	public abstract RegularFileProperty getReadme();

    @Input
	public abstract Property<ShellPluginExtension.MultiActionModeStrategy> getMultiActionModeStrategy();

	@InputFiles
	public abstract ConfigurableFileCollection getSources();

    @Input @Optional
	public abstract Property<String> getLauncherReactorScript();

    @Input @Optional
	public abstract Property<Boolean> getLauncherReactorEnvironment();

    @InputFile @Optional
	public abstract RegularFileProperty getPostInstallScript();

	@OutputFile
	public abstract RegularFileProperty getExecutorTarget();

    @OutputFile
	public abstract RegularFileProperty getTarget();

	private static final String DISPENSE_FILENAME = "dispense.sh";

	@Inject
	public DispenserTask() {
		getProjectName().convention(getProject().getName());
		getExecutorTarget().convention(getProject().getLayout().getBuildDirectory().file(ShellPackagePlugin.EXPLODED_WORK_PATH+File.separator+DISPENSE_FILENAME));
		getTarget().convention(() 
            -> getProject().getLayout().getBuildDirectory().file(ShellPackagePlugin.PLUGIN_WORK_FOLDER+"/"+ShellPackagePlugin.DISPENSER_WORK_FOLDER+"/"+buildArchiveFileName()+".sh")
            .get().getAsFile());
	}

	@TaskAction
	public void execute() {
		final String projectName = getProjectName().get();
        final Set<File> sources = getSources().getFiles();
        final File dispenserFile = getExecutorTarget().get().getAsFile();
        
        if (!dispenserFile.getParentFile().exists()) {
            dispenserFile.getParentFile().mkdirs();
        }

        final File explodedWorkFile = getProject().getLayout().getBuildDirectory().file(ShellPackagePlugin.EXPLODED_WORK_PATH).get().getAsFile();
        final File contentExplodedDirectory = new File(explodedWorkFile, "content");

        try {
            copyScriptUtils(explodedWorkFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy runtime util scripts : "+e.getMessage(), e);
        }

        if (getLauncherReactorScript().isPresent()) {
            final String reactorPath = getLauncherReactorScript().get();
            final File reactor = new File(contentExplodedDirectory, reactorPath);
            if (! reactor.exists()) {
                throw new InvalidUserDataException("Launcher script '"+reactorPath+"' does not exist");
            }
            try {
                SystemUtil.makeExecutable(reactor, false, false);
            } catch (IOException e) {
                throw new RuntimeException("Failed to make '"+reactor.getAbsolutePath()+"' : "+e.getMessage(), e);
            }
        }

        try (ShellPackageDispenserExecutorBuilder builder = new ShellPackageDispenserExecutorBuilder(getExecutorTarget().get().getAsFile() /* getProjectName().get() *//* , extension */)) {
            builder
                .packageName(getProjectName().get())
                .label(getProjectLabel().get())
                .packageVersion(getProjectVersion().getOrNull())
                .actionModeStrategy(getMultiActionModeStrategy().get())
                .showBanner(getBanner().isPresent())
                .showReadme(getReadme().isPresent())
                .executeUserScript(getPostInstallScript().isPresent())
                .launcherScript(getLauncherReactorScript().getOrNull())
                .launcherScriptHasEnvironmentProperties(getLauncherReactorEnvironment().getOrElse(false));
            builder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create dispense script : "+e.getMessage(), e);
        }

        final File archiveFile = getTarget().get().getAsFile();
        try (ShellPackageDispenserArchiveBuilder builder = new ShellPackageDispenserArchiveBuilder(explodedWorkFile, archiveFile)) {
            builder.makeExecutable(true);
            builder.build();
    		getLogger().lifecycle("Created archive file '{}'", archiveFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create archive : "+e.getMessage(), e);
        }


	}

    public void target(Provider<File> provider) {
        getExecutorTarget().fileProvider(provider);
    }
    public void target(File target) {
        getExecutorTarget().fileValue(target);
    }


    private String buildArchiveFileName() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getProjectName().get());
        if (getProjectVersion().isPresent()) {
            builder
                .append("-")
                .append(getProjectVersion().get());
        }
        return builder.toString();
    }

    private void copyScriptUtils(File explodedDir) throws IOException {
        final File scriptUtilsDir = new File(explodedDir, "util");
        Files.createDirectory(scriptUtilsDir.toPath());
        for (String resourceFileName : List.of("log.sh", "flowui-builder-json.sh", "flowui-dumbcli.sh", "flowui-humbletui.sh", "flowui.sh", "shell-util.sh")) {
            final String resourcePath = "/runtime/"+resourceFileName;
            try {
                @Cleanup
                final var inputStream = ResourceUtil.getClassPathResource(resourcePath);

                @Cleanup
                final var output = new FileOutputStream(new File(scriptUtilsDir, resourceFileName));

                inputStream.transferTo(new FileOutputStream(new File(scriptUtilsDir, resourceFileName)));
            } catch (Exception e) {
                throw new IOException("Failed to copy '"+resourcePath+"'", e);
            }
        }
    }


 
}
