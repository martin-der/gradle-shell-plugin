package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.tetrakoopa.gradle.ShellEscaper;
import net.tetrakoopa.gradle.plugin.shell.ShellPluginExtension.MultiActionModeStrategy;
import net.tetrakoopa.gradle.plugin.shell.ShellPluginExtension.TextFileSource;
import net.tetrakoopa.gradle.plugin.task.DispenserTask;
import net.tetrakoopa.gradle.plugin.task.ShellPropertiesTask;
import net.tetrakoopa.gradle.plugin.task.TextFileSourceTask;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;

/**
 * Registers the whole task graph up front, so that a build script can configure any of the tasks
 * with {@code tasks.named(...)} and so that {@code shell-build} wires itself correctly.
 *
 * <p>Tasks whose input is optional ({@code banner}, {@code readme}, {@code launcherProperties}) are
 * registered unconditionally but guarded with {@code onlyIf}. Registering them in
 * {@code afterEvaluate} instead — as this plugin used to — made them invisible to
 * {@code tasks.named()} from the build script and forced eager realisation of every provider.
 */
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




    @Override
    public void apply(Project project) {

        final ShellPluginExtension extension = project.getExtensions().create(ShellPluginExtension.NAME, ShellPluginExtension.class);

        final Internal internal = new Internal();
        final File buildDirectory = project.getLayout().getBuildDirectory().get().getAsFile();
        internal.workingDir = new File(buildDirectory, PLUGIN_WORK_FOLDER);
        internal.dispenserWorkingDir = new File(internal.workingDir, DISPENSER_WORK_FOLDER);
        final File explodedDir = new File(internal.dispenserWorkingDir, EXPLODED_WORK_FOLDER);
        internal.explodedPackageDir = explodedDir;
        final File contentDir = new File(explodedDir, "content");
        internal.contentDir = contentDir;
        final File resourceDir = new File(explodedDir, EXPLODED_RESOURCE_PATH);
        internal.resourceDir = resourceDir;

        final TaskProvider<Copy> prepareSourcesTaskProvider = project.getTasks().register("prepareSources", Copy.class, copy -> {
            copy.setGroup(DISPENSER_TASK_GROUP);
            // The property always has a value (it defaults to project.copySpec() and the DSL mutates
            // that very instance), so resolving it here is safe and picks up the user's 'from' clauses.
            copy.with(extension.getSource().get());
            copy.into(contentDir);
        });
        internal.task.prepareSources = prepareSourcesTaskProvider.get();

        final TaskProvider<TextFileSourceTask> bannerTaskProvider =
            registerTextFileTask(project, "banner", RESOURCE_PATH_BANNER,
                () -> extension.getBanner() == null ? null : extension.getBanner().getSource(),
                () -> extension.getBanner() == null ? null : extension.getBanner().getModify());

        final TaskProvider<TextFileSourceTask> readmeTaskProvider =
            registerTextFileTask(project, "readme", RESOURCE_PATH_README,
                () -> extension.getInstaller().readme == null ? null : extension.getInstaller().readme.resolve(project),
                () -> extension.getInstaller().readme == null ? null : extension.getInstaller().readme.getModify());

        final TaskProvider<ShellPropertiesTask> launcherPropertiesTaskProvider =
            project.getTasks().register("launcherProperties", ShellPropertiesTask.class, properties -> {
                properties.setGroup(DISPENSER_TASK_GROUP);
                // An empty environment is a legitimate value for @Input; onlyIf is what skips the
                // task when the user asked for no launcher environment at all.
                properties.onlyIf(spec -> !launcherEnvironment(extension).isEmpty());
                properties.getEnvironment().set(project.provider(() -> {
                    final Map<String, Object> rendered = new LinkedHashMap<>();
                    launcherEnvironment(extension).forEach((key, value) -> rendered.put(key, renderShellValue(value)));
                    return rendered;
                }));
                properties.getOutputFile().set(project.getLayout().getBuildDirectory().file(RESOURCE_PATH_LAUCNCHER_PROPERTIES));
                properties.getExportvariables().set(true);
            });

        final TaskProvider<DispenserTask> dispenserTaskProvider = project.getTasks().register("dispenser", DispenserTask.class, dispenser -> {
            dispenser.setGroup(DISPENSER_TASK_GROUP);
            final Provider<String> projectNameProvider =
                project.provider(() -> extension.getName().orElse(project.provider(project::getName)).get());
            dispenser.getMultiActionModeStrategy().set(project.provider(() ->
                extension.getAction().getMode() == null
                    ? MultiActionModeStrategy.ACTION_MODE_PREFIX
                    : extension.getAction().getMode()));
            dispenser.getDefaultAction().set(project.provider(() -> {
                final String defaultAction = extension.getAction().getDefaultAction();
                return defaultAction == null || defaultAction.isBlank() ? null : defaultAction;
            }));
            dispenser.getProjectName().set(projectNameProvider);
            dispenser.getProjectLabel().set(project.provider(() -> extension.getLabel().orElse(projectNameProvider).get()));
            dispenser.getProjectVersion().set(project.provider(() -> extension.getVersion().getOrNull()));
            dispenser.getDistributionName().set(project.provider(() -> extension.getDistributionName().getOrNull()));
            dispenser.getMakeExecutable().set(project.provider(() -> extension.getInstaller().isMakeExecutable()));
            dispenser.getContentDirectory().set(contentDir);
            dispenser.getSources().setFrom(contentDir);
            dispenser.getBanner().set(project.provider(() -> extension.getBanner() == null ? null
                : project.getLayout().getBuildDirectory().file(RESOURCE_PATH_BANNER).get()));
            dispenser.getReadme().set(project.provider(() -> extension.getInstaller().readme == null ? null
                : project.getLayout().getBuildDirectory().file(RESOURCE_PATH_README).get()));
            dispenser.getLauncherReactorScript().set(project.provider(() -> extension.getLauncher() == null ? null
                : extension.getLauncher().getScript()));
            dispenser.getLauncherReactorEnvironment().set(project.provider(() ->
                extension.getLauncher() != null && !extension.getLauncher().getEnvironment().isEmpty()));
            dispenser.getUsePersistentTemporaryDirectory().set(project.provider(extension::isKeepTemporaryDirectory));
        });
        final DispenserTask dispenserTask = internal.task.dispenserTask = dispenserTaskProvider.get();

        dispenserTask.dependsOn(internal.task.prepareSources);
        dispenserTask.dependsOn(bannerTaskProvider);
        dispenserTask.dependsOn(readmeTaskProvider);
        dispenserTask.dependsOn(launcherPropertiesTaskProvider);
        internal.task.prepareBanner = bannerTaskProvider.get();
        internal.task.prepareReadme = readmeTaskProvider.get();
        internal.task.prepareLauncherProperties = launcherPropertiesTaskProvider.get();

        final TaskProvider<Task> buildProvider = project.getTasks().register("shell-build", Task.class, task -> {
            task.setGroup(DISPENSER_TASK_GROUP);
            task.setDescription("Builds the self-extracting shell package");
        });
        buildProvider.get().dependsOn(dispenserTask);

        project.afterEvaluate(p -> {
            extension.validate();
        });

    }

    /**
     * Registers a {@code source -> destination} text task that only runs when the user actually
     * configured that piece of content.
     */
    private TaskProvider<TextFileSourceTask> registerTextFileTask(
        Project project,
        String taskName,
        String destinationPath,
        Supplier<File> source,
        Supplier<Function<String, String>> modify) {

        return project.getTasks().register(taskName, TextFileSourceTask.class, task -> {
            task.setGroup(DISPENSER_TASK_GROUP);
            task.onlyIf(spec -> source.get() != null);
            task.getSourceFile().set(project.getLayout().file(project.provider(source::get)));
            task.getDestinationFile().set(project.getLayout().getBuildDirectory().file(destinationPath));
            task.modify(project.provider(modify::get));
            task.getTransformationDescription().set(
                project.provider(() -> TextFileSourceTask.describe(modify.get())));
        });
    }

    private static Map<String, String> launcherEnvironment(ShellPluginExtension extension) {
        return extension.getLauncher() == null ? Map.of() : extension.getLauncher().getEnvironment();
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
