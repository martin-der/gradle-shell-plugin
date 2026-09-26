package net.tetrakoopa.gradle.plugin.task;


import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskAction;

import net.tetrakoopa.gradle.GenerationHelper;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * Writes a shell file that assigns the launcher environment, and exports it.
 *
 * <p>The result is {@code source}d by the generated installer, so every key has to be a valid bash
 * identifier and every value has to survive being written on a single unquoted line. Anything else
 * is reported as a configuration error rather than shipped as a package that dies on startup.
 */
public abstract class ShellPropertiesTask extends DefaultTask {

    /** Bash identifiers only. */
    private static final Pattern SHELL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*\\z");

    private final MapProperty<String, Object> environment;

    @Input
    public abstract Property<Boolean> getExportvariables();

    @Input
    public MapProperty<String, Object> getEnvironment() {
		return environment;
	};

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

	@Inject
    public ShellPropertiesTask(ObjectFactory objects, Project project) {
        environment = objects.mapProperty(String.class, Object.class);
    }

    @TaskAction
    public void createEnvFile() throws IOException {

		final var finalOutput = getOutputFile().getAsFile().get();
        finalOutput.getParentFile().mkdirs();

		final var finalEnvironment = getEnvironment().get();

		try(final var generator = new GenerationHelper.PropertiesGenerator(new FileWriter(finalOutput))) {
            generator.exportVariables(getExportvariables().get());
            boolean isFirst = true;
            for (Map.Entry<String, Object> entry : finalEnvironment.entrySet()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    generator.append("\n");
                }
				generator.append(entry.getKey(), asShellValue(entry.getKey(), entry.getValue()));
			}
		}
    }

	/**
	 * Checks a value that is already rendered as a shell word and passes it through unchanged.
	 *
	 * <p>Rendering is the caller's job ({@code ShellPackagePlugin#renderShellValue}) because only the
	 * caller knows which parts of a value are meant to stay inert and which are meant to expand when
	 * the installer sources this file.
	 */
	private static String asShellValue(String name, Object value) {
		if (name == null || !SHELL_IDENTIFIER.matcher(name).matches()) {
			throw new InvalidUserDataException(
				"'" + name + "' is not a valid shell variable name (expected [A-Za-z_][A-Za-z0-9_]*). "
				+ "Names are written verbatim into a file that the generated installer sources.");
		}
		if (value == null) {
			return null;
		}
		final String string = String.valueOf(value);
		if (string.indexOf('\n') >= 0 || string.indexOf('\r') >= 0) {
			throw new InvalidUserDataException(
				"Value of '" + name + "' spans several lines. This file is read one assignment per line by the "
				+ "generated installer, so a line break would be read as a new, invalid assignment.");
		}
		return string;
	}
}
