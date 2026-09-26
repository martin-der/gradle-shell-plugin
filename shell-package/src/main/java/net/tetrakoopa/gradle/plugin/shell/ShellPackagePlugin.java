package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.tetrakoopa.gradle.ShellEscaper;
import net.tetrakoopa.gradle.plugin.exception.ShellPackagePluginException;
import net.tetrakoopa.gradle.plugin.shell.ShellPluginExtension.MultiActionModeStrategy;
import net.tetrakoopa.gradle.plugin.task.DispenserTask;
import net.tetrakoopa.gradle.plugin.task.ShellPropertiesTask;
import net.tetrakoopa.gradle.plugin.task.TextFileSourceTask;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;

public class ShellPackagePlugin implements Plugin<Project> {

 	private static final String DOCUMENTATION_TASK_GROUP = "documentation";
	private static final String DISPENSER_TASK_GROUP = "shell";


    public static final String PLUGIN_WORK_FOLDER = "shell";
    public static final String DISPENSER_WORK_FOLDER = "dispenser";
    public static final String EXPLODED_WORK_FOLDER = "exploded";
    public static final String EXPLODED_WORK_PATH = PLUGIN_WORK_FOLDER+File.separator+DISPENSER_WORK_FOLDER+File.separator+EXPLODED_WORK_FOLDER;
    public static final String EXPLODED_RESOURCE_PATH = "resource";

    private static final String RESOURCE_PATH_BANNER = ShellPackagePlugin.EXPLODED_WORK_PATH+File.separator+EXPLODED_RESOURCE_PATH+File.separator+"banner.txt";
    private static final String RESOURCE_PATH_LAUCNCHER_PROPERTIES = ShellPackagePlugin.EXPLODED_WORK_PATH+File.separator+EXPLODED_RESOURCE_PATH+File.separator+"launcher-properties.sh";
    private static final String RESOURCE_PATH_README = ShellPackagePlugin.EXPLODED_WORK_PATH+File.separator+EXPLODED_RESOURCE_PATH+File.separator+"readme.txt";

    /** Substituted by the user in a launcher environment value; see {@link #renderShellValue}. */
    static final String CONTENT_DIRECTORY_PLACEHOLDER = "{{MDU-SD_CONTENT-DIRECTORY}}";

    /** Shell variable the generated installer exports, holding the extraction directory. */
    private static final String CONTENT_DIRECTORY_REFERENCE = "${MDU_SD_DISPENSER_CONTENT_DIRECTORY}";
    


    private static final UnaryOperator<String> identityFunction = UnaryOperator.identity();



    @Override
    public void apply(Project project) {

        final ShellPluginExtension extension = project.getExtensions().create(ShellPluginExtension.NAME, ShellPluginExtension.class);
     
        final Internal internal = new Internal();
        internal.workingDir = new File(project.getLayout().getBuildDirectory().get().getAsFile(), PLUGIN_WORK_FOLDER);
        final File dispenserWorkingDir = new File(internal.workingDir, DISPENSER_WORK_FOLDER);
        internal.dispenserWorkingDir = dispenserWorkingDir;
        final File explodedDir = new File(dispenserWorkingDir, "exploded");
        internal.explodedPackageDir = explodedDir;
        final File contentDir = new File(explodedDir, "content");
        internal.contentDir = contentDir;
        final File resourceDir = new File(explodedDir, EXPLODED_RESOURCE_PATH);
        internal.resourceDir = resourceDir;

        {
            final TaskProvider<Copy> prepareSourcesTaskProvider = project.getTasks().register("prepareSources", Copy.class, copy -> {
                copy.with(project.provider(() -> extension.getSource().get()).get());
                copy.into(contentDir);
            });
            internal.task.prepareSources = prepareSourcesTaskProvider.get();
        }

        final TaskProvider<DispenserTask> dispenserTaskProvider = project.getTasks().register("dispenser", DispenserTask.class, dispenser -> {
            final Provider<String> projectNameProvider = project.provider(() -> extension.getName().orElse(project.provider(() -> project.getName())).get());
            dispenser.getMultiActionModeStrategy().set(project.provider(() -> extension.getAction().getMode()));
            dispenser.getProjectName().set(projectNameProvider);
            dispenser.getProjectLabel().set(project.provider(() -> extension.getLabel().orElse(projectNameProvider).get()));
            dispenser.getProjectVersion().set(project.provider(() -> extension.getVersion().getOrNull()));
            dispenser.getBanner().set(project.provider(() -> extension.getBanner() == null ? null : project.getLayout().getBuildDirectory().file(RESOURCE_PATH_BANNER).get()));
            dispenser.getReadme().set(project.provider(() -> extension.getInstaller().readme == null ? null : project.getLayout().getBuildDirectory().file(RESOURCE_PATH_README).get()));
            dispenser.getLauncherReactorScript().set(project.provider(() -> extension.getLauncher() == null ? null : extension.getLauncher().getScript()));
            dispenser.getLauncherReactorEnvironment().set(project.provider(() -> extension.getLauncher() == null ? false : !extension.getLauncher().getEnvironment().isEmpty()));
            dispenser.getUsePersistentTemporaryDirectory().set(project.provider(() -> extension.isKeepTemporaryDirectory()));
        });
        final DispenserTask dispenserTask = internal.task.dispenserTask = dispenserTaskProvider.get();

        dispenserTask.setGroup(DISPENSER_TASK_GROUP);
        dispenserTask.dependsOn(internal.task.prepareSources);
        internal.task.dispenserTask = dispenserTask;


        final TaskProvider<Task> buildProvider = project.getTasks().register("shell-build", Task.class);
        final Task buildTask = buildProvider.get();
        buildTask.setGroup(DISPENSER_TASK_GROUP);
        buildTask.dependsOn(dispenserTask);
        

        project.afterEvaluate(p -> {
            postEvaluateSanityCheck(extension);
            addTasks(project, extension, internal);
        });

    }

    private void addTasks(Project project, ShellPluginExtension extension, Internal internal) {

        internal.workingDir = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "shell");

        if (extension.getDistributionName() != null) {
            internal.name = extension.getDistributionName().getOrNull();
        } else if (extension.getName().isPresent()) {
            internal.name = extension.getName().get();
        } else {
            internal.name = project.getName();
        }

        {
            if (extension.getBanner() != null) {
                final TaskProvider<TextFileSourceTask> prepareSourcesTaskProvider = project.getTasks().register("banner", TextFileSourceTask.class, banner -> {
                    banner.modify(project.provider(() -> {
                        final var modify = extension.getBanner().getModify();
                        return modify == null ? identityFunction : modify;
                    }).get());
                    banner.getSourceFile().set(project.provider(() -> extension.getBanner().getSource()).get());
                    banner.getDestinationFile().set(project.provider(() -> project.getLayout().getBuildDirectory().file(RESOURCE_PATH_BANNER)).get());

                });
                internal.task.prepareBanner = prepareSourcesTaskProvider.get();
                internal.task.prepareBanner.setGroup(DISPENSER_TASK_GROUP);
                internal.task.dispenserTask.dependsOn(internal.task.prepareBanner);
            }
        }
        {
            if (extension.getInstaller().readme != null) {
                final TaskProvider<TextFileSourceTask> prepareSourcesTaskProvider = project.getTasks().register("readme", TextFileSourceTask.class, readme -> {
                    readme.modify(project.provider(() -> {
                        final var modify = extension.getInstaller().readme.getModify();
                        return modify == null ? identityFunction : modify;
                    }).get());
                    readme.getSourceFile().set(project.provider(() -> extension.getInstaller().readme.resolve(project)).get());
                    readme.getDestinationFile().set(project.provider(() -> project.getLayout().getBuildDirectory().file(RESOURCE_PATH_README)).get());

                });
                final var task = prepareSourcesTaskProvider.get();
                task.setGroup(DISPENSER_TASK_GROUP);
                internal.task.dispenserTask.dependsOn(task);
            }
        }
        {
            if (extension.getLauncher() != null) {
                final var launcher = extension.getLauncher();
                if (!launcher.getEnvironment().isEmpty()) {
                    final TaskProvider<ShellPropertiesTask> prepareLauncherPropertiesTaskProvider = project.getTasks().register("launcherProperties", ShellPropertiesTask.class, properties -> {
                        properties.getEnvironment().set(project.provider(() -> {
                            final Map<String, Object> replacedMap = new HashMap<String, Object>();
                            extension.getLauncher().getEnvironment().forEach((key, value) -> {
                                replacedMap.put(key, renderShellValue(value));
                            });
                            return replacedMap;
                        }).get());
                        properties.getOutputFile().set(project.provider(() -> project.getLayout().getBuildDirectory().file(RESOURCE_PATH_LAUCNCHER_PROPERTIES)).get());
                        properties.getExportvariables().set(true);

                    });
                    internal.task.prepareLauncherProperties = prepareLauncherPropertiesTaskProvider.get();
                    internal.task.prepareLauncherProperties.setGroup(DISPENSER_TASK_GROUP);
                    internal.task.dispenserTask.dependsOn(internal.task.prepareLauncherProperties);
                }
            }
        }

    }

    private void postEvaluateSanityCheck(ShellPluginExtension extension) {
        if (extension.launcher != null) {
            final ShellPluginExtension.Launcher launcher = extension.launcher;
            if (launcher.getScript() == null || launcher.getScript().isEmpty()) {
                throw new ShellPackagePluginException("If a launcher is requested then a path to a the script to execute is required with 'launcher.script'");
            }
        }

        final var action = extension.getAction();
        if (action.getMode() == null) {
            action.setMode(MultiActionModeStrategy.ACTION_MODE_PREFIX.getCode());
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

    /**
     * Renders a launcher environment value as a single shell word.
     *
     * <p>The result is written verbatim into a file the generated installer {@code source}s, so it
     * has to satisfy two opposing requirements:
     *
     * <ul>
     * <li>arbitrary user text must be inert — a value such as {@code "a; rm -rf /"} or
     *     {@code "Foo Bar"} must not split into several words or inject a command, so it is
     *     single-quoted;</li>
     * <li>{@value #CONTENT_DIRECTORY_PLACEHOLDER} has to expand to the extraction directory,
     *     which only exists at run time, so the reference substituted for it must be
     *     <em>double</em>-quoted.</li>
     * </ul>
     *
     * <p>Both are met by quoting each literal segment on its own and emitting the substituted
     * reference as a double-quoted word between them: adjacent quoted words concatenate in bash, so
     * {@code 'a'"$HOME"'b'} is a single word with the value {@code a$HOMEb}. A value that is exactly
     * the placeholder therefore renders as {@code "${MDU_SD_DISPENSER_CONTENT_DIRECTORY}"}, and a
     * value that does not mention it is byte-for-byte what {@link ShellEscaper#quote} produces.
     *
     * <p>Keys and values are checked in {@code ShellPluginExtension#validate()} before we get here.
     */
    static String renderShellValue(String value) {
        if (value == null) {
            return null;
        }
        final StringBuilder rendered = new StringBuilder(value.length() + 8);
        int from = 0;
        int placeholder;
        while ((placeholder = value.indexOf(CONTENT_DIRECTORY_PLACEHOLDER, from)) >= 0) {
            if (placeholder > from) {
                rendered.append(ShellEscaper.quote(value.substring(from, placeholder)));
            }
            rendered.append("\"").append(CONTENT_DIRECTORY_REFERENCE).append("\"");
            from = placeholder + CONTENT_DIRECTORY_PLACEHOLDER.length();
        }
        if (from < value.length()) {
            rendered.append(ShellEscaper.quote(value.substring(from)));
        }
        return rendered.toString();
    }
    

}
