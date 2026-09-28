package net.tetrakoopa.gradle.plugin.task;


import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.util.function.Function;

/**
 * Copies a text file, optionally rewriting each line.
 *
 * <p>The transformation is supplied as a {@link Provider} rather than a value so that it is
 * resolved when Gradle snapshots the task's inputs, not when the task happens to be realised. With
 * an eager value, a task realised early (say by {@code gradle tasks}) would permanently record the
 * identity transformation and then never re-run when a real {@code modify} closure is configured.
 */
public abstract class TextFileSourceTask extends DefaultTask {

    @InputFile @Optional
    public abstract RegularFileProperty getSourceFile();

    /**
     * Human readable fingerprint of the transformation, part of the task's input snapshot.
     */
    @Input
    public abstract Property<String> getTransformationDescription();

    @OutputFile
    public abstract RegularFileProperty getDestinationFile();

    private Provider<Function<String, String>> transformation;

    public void modify(Provider<Function<String, String>> transformer) {
        this.transformation = transformer;
    }

    @TaskAction
    public void processFile() {
        if (!getSourceFile().isPresent()) {
            // Guarded by onlyIf in the plugin; kept so a stray direct invocation cannot NPE.
            return;
        }
        final File source = getSourceFile().get().getAsFile();
        final File destination = getDestinationFile().get().getAsFile();

        destination.getParentFile().mkdirs();

        final Function<String, String> transformer = transformation == null ? null : transformation.getOrNull();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination));
             BufferedReader reader = new BufferedReader(new FileReader(source))) {

            String line;
            while ((line = reader.readLine()) != null) {
				final String processedLine = transformer == null ? line : transformer.apply(line);
                if (processedLine != null) {
                    writer.write(processedLine);
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            throw new GradleException("Failed to copy '"+source.getAbsolutePath()+"' to '"+destination.getAbsolutePath()+"'", e);
        }

    }

    /**
     * Builds a stable-ish description of a transformation for the task's input snapshot.
     *
     * <p>A lambda's {@code toString()} is not guaranteed to be stable across JVM runs, which would
     * make the task never up-to-date, so lambdas are reduced to a marker instead.
     */
    public static String describe(Function<String, String> transformer) {
        if (transformer == null) {
            return "identity";
        }
        final String toString = String.valueOf(transformer);
        if (toString.contains("$$Lambda$")) {
            return "lambda";
        }
        return toString;
    }

}
