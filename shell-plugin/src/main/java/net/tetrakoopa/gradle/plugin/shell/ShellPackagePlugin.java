package net.tetrakoopa.gradle.plugin.shell;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import lombok.Cleanup;
import net.tetrakoopa.gradle.SystemUtil;
import net.tetrakoopa.gradle.plugin.exception.ShellPackagePluginException;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;

public class ShellPackagePlugin implements Plugin<Project> {

 	private static final String DOCUMENTATION_TASK_GROUP = "documentation";
	private static final String DISPENSER_TASK_GROUP = "shell";
    private static final String createShellPackageCommand = "shar";



    @Override
    public void apply(Project project) {

        // project.getExtensions().add(ShellPluginExtension.NAME, ShellPluginExtension.class);

        final ShellPluginExtension extension = project.getExtensions().create(ShellPluginExtension.NAME, ShellPluginExtension.class);
        // final ShellPluginExtension0 extension00 = project.getExtensions().create(ShellPluginExtension.NAME, ShellPluginExtension0.class);
        // project.getExtensions().add(ShellPluginExtension.NAME, ShellPluginExtension.class);
        // final ShellPluginExtension extension = project.getExtensions().getByType(ShellPluginExtension.class);
        

        // dispenserIntermediateDir.mkdirs();

        // final Task packaage = project.getTasks().register("package", task -> {

        //     // ShellPluginExtension extension = project.getExtensions().create("shell_package", ShellPluginExtension.class);
        //     final ShellPluginExtension extension2 = project.getExtensions().findByType(ShellPluginExtension.class);

        //     task.doLast(s -> {
        //         System.out.println("Hello from plugin 'shell' : "+extension.getName());
        //         System.out.println("Hello from plugin 'shell' : "+extension.getInstaller().readme.getPath());
        //     });
        // }).get();
        
        // System.out.println("extension.source : "+extension.source);

        project.afterEvaluate(p -> {
            postEvaluateSanityCheck(extension);
            addTasks(project, extension);
        });



        // project.afterEvaluate(new Closure<Project>(project) {

        //     public void doCall() {
        //         project.getTasksByName("build", true).forEach(t -> t.dependsOn(dispenser));

        //     }

        // });
        // dispenser.dependsOn("build");



        // project.afterEvaluate(new Closure(project) {
        //     final Task build = project.getTasksByName("build", false).iterator().next();

        //     @Override
        //     public Object call() {
        //         dispenser.dependsOn(build);
        //         packaage.dependsOn(build);
        //         return super.call();
        //     }
        // });

    }

    private void addTasks(Project project, ShellPluginExtension extension) {

        final Internal internal = new Internal();
        internal.workingDir = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "shell");

        if (extension.getDistributionName() != null) {
            internal.name = extension.getDistributionName();
        } else if (extension.getName() != null) {
            internal.name = extension.getName().get();
        } else {
            internal.name = project.getName();
        }

        final File dispenserWorkingDir = new File(internal.workingDir, "dispenser");
        final File explodedDir = new File(dispenserWorkingDir, "exploded");
        internal.explodedPackageDir = explodedDir;
        final File contentDir = new File(explodedDir, "content");
        internal.contentDir = contentDir;

        final Copy prepareSourcesTask;
        {
            final TaskProvider<Copy> prepareSourcesTaskProvider = project.getTasks().register("prepareSources", Copy.class);
            prepareSourcesTaskProvider.configure(copy -> {
                    copy.with(extension.getSource().get());
                    copy.into(contentDir);
            });
            prepareSourcesTask = prepareSourcesTaskProvider.get();
        }
        prepareSourcesTask.setGroup(DISPENSER_TASK_GROUP);

                
        final Task dispenserTask = project.getTasks().register("dispenser", task -> {

            task.doLast(s -> {

                final String fileName;
                if (extension.getDistributionName() != null) {
                    fileName = extension.getDistributionName();
                } else if (extension.getName() != null) {
                    fileName = extension.getName().get();
                } else {
                    fileName = project.getName();
                }

                try {
                    copyScriptUtils(explodedDir);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy runtime util scripts : "+e.getMessage(), e);
                }

                try {
                    copyResources(explodedDir, project, extension, internal);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy runtime util scripts : "+e.getMessage(), e);
                }

                try (ShellPackageDispenserBuilder builder = new ShellPackageDispenserBuilder(project, internal, extension)) {
                    builder.build();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create dispense script : "+e.getMessage(), e);
                }

                final File archiveFile = new File(dispenserWorkingDir, fileName+".shar");
                createArchive(archiveFile, extension, internal);

                try {
                    SystemUtil.makeExecutable(archiveFile, true, false);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to make dispense executable : "+e.getMessage(), e);
                }

                postackageCreationSanityCheck(extension, internal);

            });
        }).get();


        dispenserTask.setGroup(DISPENSER_TASK_GROUP);
        dispenserTask.dependsOn(prepareSourcesTask);

    }

    private void postEvaluateSanityCheck(ShellPluginExtension extension) {
        if (extension.launcher != null) {
            final ShellPluginExtension.Launcher launcher = extension.launcher;
            if (launcher.getScript() == null || launcher.getScript().isEmpty()) {
                throw new ShellPackagePluginException("If a launcher is requested then a path to a the script to execute is required with 'launcher.script'");
            }
        }
    }

    private void postackageCreationSanityCheck(ShellPluginExtension extension, Internal internal) {
        if (extension.launcher != null) {
            final ShellPluginExtension.Launcher launcher = extension.launcher;
            if (!new File(internal.contentDir, launcher.getScript()).exists()) {
                throw new ShellPackagePluginException("No such script '"+extension.launcher.getScript()+"', specified as launcher, is packaged");
            }
        }
    }

    private void createArchive(File archiveFile, ShellPluginExtension extension, final Internal internal) {
        final File dispenserFile = archiveFile;
        final String author = extension.information.maintainer != null && extension.information.maintainer.name != null && !extension.information.maintainer.name.isBlank() 
            ? extension.information.maintainer.name.trim() : null;
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
            } catch (IOException e) {
                throw new IOException("Failed to copy '"+resourcePath+"'", e);
            }
        }
    }

    private void copyResources(File explodedDir, Project project, ShellPluginExtension extension, Internal internal) throws IOException {
        final AtomicBoolean directoryCreateed = new AtomicBoolean();

        final File resourceDir = new File(explodedDir, "resource");

        final Runnable createDirectoryIfNeeded = () -> {
            if (directoryCreateed.compareAndSet(false, true)) {
                try {
                    Files.createDirectories(resourceDir.toPath());
                } catch (Exception ex) {
                    throw new ShellPackagePluginException("Failed to create resource directory '"+resourceDir.getAbsolutePath()+"' : "+ex.getMessage(), ex);
                }
            }
        };

        if (extension.banner.content.isDefined()) {
            createDirectoryIfNeeded.run();

            final File banner = extension.banner.content.resolve(project);

            copyResource(banner, new File(resourceDir, "banner.txt"), "banner");
        }

        if (extension.installer.readme.isDefined()) {
            createDirectoryIfNeeded.run();

            final File readme = extension.installer.readme.resolve(project);

            copyResource(readme, new File(resourceDir, "README.md"), "readme");
        }

        if (extension.launcher != null) {
            final var environment = extension.launcher.getEnvironment();
            if (!environment.isEmpty()) {
                createDirectoryIfNeeded.run();

                @Cleanup
                final FileOutputStream environmentFile = new FileOutputStream(new File(resourceDir, "environment.sh"));

                for (var entry : environment.entrySet()) {
                    final String key = entry.getKey();
                    final String value = replaceValues(entry.getValue(), internal);
                    environmentFile.write(("export "+key+"=\""+value+"\"\n").getBytes(StandardCharsets.UTF_8));
                };
            }
        }
    }

    private void copyResource(File inputFile, File outputFile, String resource) throws IOException {

            final FileInputStream input;
            try {
                input = new FileInputStream(inputFile);
            } catch (FileNotFoundException fnex) {
                throw new ShellPackagePluginException("Failed to produce resource '"+resource+"' : Source file not found : "+fnex.getMessage(), fnex);
            }
            try {
                @Cleanup
                final FileOutputStream output = new FileOutputStream(outputFile);
                input.transferTo(output);

            } finally {
                input.close();
            }
    }

    private String replaceValues(String string, Internal internal) {
        return string
            .replace("{{CONTENT-DIRECTORY}}", "${MDU_DISPENSER_CONTENT_DIRECTORY}");
    }
    

}
