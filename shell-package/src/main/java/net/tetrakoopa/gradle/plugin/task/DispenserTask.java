package net.tetrakoopa.gradle.plugin.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
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
		        final String fileName = getProjectName().get();
                // if (extension.getDistributionName() != null) {
                //     fileName = extension.getDistributionName();
                // } else if (extension.getName() != null) {
                //     fileName = extension.getName().get();
                // } else {
                //     fileName = project.getName();
                // }

                try {
                    copyScriptUtils(explodedWorkFile);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy runtime util scripts : "+e.getMessage(), e);
                }

                // try {
                //     copyResources(internal.explodedPackageDir, extension, internal);
                // } catch (IOException e) {
                //     throw new RuntimeException("Failed to copy runtime resources : "+e.getMessage(), e);
                // }

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

                try (ShellPackageDispenserArchiveBuilder builder = new ShellPackageDispenserArchiveBuilder(explodedWorkFile, getTarget().get().getAsFile())) {
                    builder.makeExecutable(true);
                    builder.build();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create archive : "+e.getMessage(), e);
                }

                // final File archiveFile = new File(internal.dispenserWorkingDir, fileName+".shar");
                // createArchive(archiveFile, extension, internal);

                // try {
                //     SystemUtil.makeExecutable(archiveFile, true, false);
                // } catch (IOException e) {
                //     throw new RuntimeException("Failed to make dispense executable : "+e.getMessage(), e);
                // }

                // postackageCreationSanityCheck(extension, internal);


	}

    public void target(Provider<File> provider) {
        getExecutorTarget().fileProvider(provider);
    }
    public void target(File target) {
        getExecutorTarget().fileValue(target);
    }

    private void createArchive(File archiveFile, ShellPluginExtension extension) {
        /*
        final File dispenserFile = archiveFile;
        final String author = getAuthor().getOrNull();
        final FileOutputStream dispenserInputStream;

        try {
            dispenserInputStream = new FileOutputStream(dispenserFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to open dispenser file '"+dispenserFile.getAbsolutePath()+"' from wrting : "+e.getMessage(), e);
        }

        try {


            try {
                this.getClass().getResourceAsStream("/dispenser/template/extract-pre.sh").transferTo(dispenserInputStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to insert '/dispenser/template/extract-pre.sh'", e);
            }

            final ProcessBuilder builder = new ProcessBuilder();
            builder.directory(internal.explodedPackageDir.getAbsoluteFile());
            builder.command(createShellPackageCommand, "--quiet", "--quiet-unshar", "--submitter="+(author != null ? author : ""), ".");
            
            Process process;
            try {
                process = builder.start();

                boolean trimingCommentAtStart = true;
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    
                    final Pattern patternComment = Pattern.compile("^#.*");
                    final Pattern patternExit = Pattern.compile("^exit[ ]+0");
                    final Pattern patternEmpty = Pattern.compile("^[ \t]*");

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (trimingCommentAtStart) {
                            if (patternComment.matcher(line).matches()) {
                                continue;
                            }
                            trimingCommentAtStart = false;
                        }
                        if (patternExit.matcher(line).matches()) {
                            final var byteBuffer = new ByteArrayOutputStream();
                            final var buffer = new BufferedOutputStream(byteBuffer);
                            buffer.write(line.getBytes());
                            buffer.write('\n');
                            boolean oneOrMoreNonEmptyLine = false;
                            while ((line = reader.readLine()) != null) {
                                buffer.write(line.getBytes());
                                buffer.write('\n');
                                if (!patternEmpty.matcher(line).matches()) {
                                    oneOrMoreNonEmptyLine = true;
                                    break;
                                }
                                oneOrMoreNonEmptyLine = true;
                            }
                            if (oneOrMoreNonEmptyLine) {
                                dispenserInputStream.write(byteBuffer.toByteArray());
                                dispenserInputStream.write('\n');
                            } else {
                                break;
                            }
                        } else {
                            dispenserInputStream.write(line.getBytes());
                            dispenserInputStream.write('\n');
                        }
                    }
                }

                final int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Command '"+createShellPackageCommand+"' exited with "+exitCode);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {
                this.getClass().getResourceAsStream("/dispenser/template/extract-post.sh").transferTo(dispenserInputStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to insert '/dispenser/template/extract-post.sh'", e);
            }

        } finally {
            try {
                dispenserInputStream.close();
            } catch (IOException e) {
                    throw new RuntimeException("Failed to finalise dispenser file '"+dispenserFile.getAbsolutePath()+"' : "+e.getMessage(), e);
            }
        }
        */
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
