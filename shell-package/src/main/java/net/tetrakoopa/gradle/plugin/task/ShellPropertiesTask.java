package net.tetrakoopa.gradle.plugin.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.TaskAction;

import net.tetrakoopa.gradle.GenerationHelper;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;


public abstract class ShellPropertiesTask extends DefaultTask {
    
    private MapProperty<String, Object> environment;
    // private RegularFileProperty outputFile;
    
    @Input
    public MapProperty<String, Object> getEnvironment() {
		return environment;
	};
    
    // public void setVariables(Property<Map<String, Object>> variables) {
    //     this.variables = variables;
    // }
    
    @OutputFile
    public abstract RegularFileProperty getOutputFile();
    
    // public void setOutputFile(RegularFileProperty outputFile) {
    //     this.outputFile = outputFile;
    // }
    
    // public void setOutputFile(String filePath) {
    //     this.outputFile = new RegularFileProperty(filePath);
    // }
    
	@Inject
    public ShellPropertiesTask(ObjectFactory objects, Project project) {
        environment = objects.mapProperty(String.class, Object.class);
    }

    @TaskAction
    public void createEnvFile() throws IOException {
        // if (environment == null) {
        //     throw new IllegalArgumentException("Variables map cannot be null or empty");
        // }
        // if (outputFile == null) {
        //     throw new IllegalArgumentException("Output file must be specified");
        // }

		final var finalOutput = getOutputFile().getAsFile().get();
        finalOutput.getParentFile().mkdirs();

		final var finalEnvironment = getEnvironment().get();

		try(final var generator = new GenerationHelper.PropertiesGenerator(new FileWriter(finalOutput))) {
            for (Map.Entry<String, Object> entry : finalEnvironment.entrySet()) {
				generator.append(entry.getKey(), entry.getValue());
			}
		}

		getLogger().lifecycle("Created environment file with {} variable(s) at: {}", finalEnvironment.size(), finalOutput.getAbsolutePath());
    }
}