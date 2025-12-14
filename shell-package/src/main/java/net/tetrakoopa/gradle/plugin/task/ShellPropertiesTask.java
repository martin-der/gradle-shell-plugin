package net.tetrakoopa.gradle.plugin.task;

import org.gradle.api.DefaultTask;
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

import javax.inject.Inject;


public abstract class ShellPropertiesTask extends DefaultTask {
    
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
				generator.append(entry.getKey(), entry.getValue());
			}
		}
    }
}