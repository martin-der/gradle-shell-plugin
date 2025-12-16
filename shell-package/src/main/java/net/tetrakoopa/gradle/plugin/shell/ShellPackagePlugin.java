package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
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
                                replacedMap.put(key, replaceValues(value, internal));
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

    private String replaceValues(String string, Internal internal) {
        return string
            .replace("{{MDU-SD_CONTENT-DIRECTORY}}", "${MDU_DISPENSER_CONTENT_DIRECTORY}");
    }
    

}
