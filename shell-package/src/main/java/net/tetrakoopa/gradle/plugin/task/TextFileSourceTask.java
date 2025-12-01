package net.tetrakoopa.gradle.plugin.task;


import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import net.tetrakoopa.gradle.plugin.shell.ShellPackagePlugin;

import org.gradle.api.provider.Provider;

import java.io.*;
import java.util.function.Function;

public abstract class TextFileSourceTask extends DefaultTask {
    
    @InputFile
    public abstract RegularFileProperty getSourceFile();
    
    private String transformationDescription = "";
    
    @Input
    public final String getTransformationDescriptions() {
        return transformationDescription;
    }

    private Function<String, String> transformation;
    
    @OutputFile
    public abstract RegularFileProperty getDestinationFile();
    // @OutputFile
    // public Provider<RegularFile> getDestinationFile() {
    //     return getProject().getLayout().getBuildDirectory().file(ShellPackagePlugin.EXPLODED_WORK_PATH+File.separator+"banner.txt");
    // }
    
    public void modify(Function<String, String> transformer) {
        transformationDescription = generateTransformationHashDescription(transformer);
        transformation = transformer;
    }
    
    @TaskAction
    public void processFile() {
        final File source = getSourceFile().get().getAsFile();
        final File destination = getDestinationFile().get().getAsFile();
        
        destination.getParentFile().mkdirs();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination));
             BufferedReader reader = new BufferedReader(new FileReader(source))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
				final String processedLine =  transformation==null ? line : transformation.apply(line);
                if (processedLine != null) {
                    writer.write(processedLine);
                    writer.newLine();
                }
            }
            
        } catch (IOException e) {
            throw new GradleException("Failed to copy '"+source.getAbsolutePath()+"' to '"+destination.getAbsolutePath()+"'", e);
        }
        
    }    

    private static String generateTransformationHashDescription(Function<String, String> transformer) {
        final String toString = transformer.toString();
        
        if (toString.contains("$$Lambda$")) {
            return "lambda@" + Integer.toHexString(transformer.hashCode());
        } else {
            return toString;
        }
    }
    
}