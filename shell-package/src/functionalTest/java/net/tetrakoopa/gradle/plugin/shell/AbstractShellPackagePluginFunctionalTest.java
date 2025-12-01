package net.tetrakoopa.gradle.plugin.shell;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import net.tetrakoopa.gradle.plugin.common.IOUtil;


public class AbstractShellPackagePluginFunctionalTest {

	protected static final File projectDir = new File("build/functionalTest");
    protected static final File buildDir = new File("build/functionalTest/build");
    protected static final File buildsDir = new File(projectDir, "builds");

	protected File testBuildDir;

	protected void createFile(File file, String content) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

	protected void createProjectFile(String filePath, String content) throws IOException {
       createFile(new File(projectDir, filePath), content);
    }

	protected void copyProjectDirectory(String projectName, String source, String target) throws IOException {
		IOUtil.copyDirectory(projectResource(projectName, source), new File(projectDir, target));  
	}
	protected void copyProjectFile(String projectName, String source, String target) throws IOException {
		IOUtil.copyFile(projectResource(projectName, source), new File(projectDir, target));  
	}

	protected File explodedFile(String filePath) {
       return new File(testBuildDir, "shell/dispenser/exploded/"+filePath);
    }
	protected boolean explodedFileExists(String filePath) {
       return explodedFile(filePath).exists();
    }
	protected InputStream explodedInputStream(String filePath) throws FileNotFoundException {
       return new FileInputStream(explodedFile(filePath));
    }
	protected String explodedTextContent(String filePath) throws IOException {
        try (InputStream stream = explodedInputStream(filePath)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

	// private String findCallingTestFunctionName() {
	// 	final var stack = Thread.currentThread().getStackTrace();
	// 	return stack[3].getMethodName();
	// }
	
	// protected String testFolderName() {
	// 	return this.getClass().getSimpleName()+"$"+findCallingTestFunctionName();
	// }

	private File projectResource(String projectName, String resourcePath) {
		if (projectName.contains("/")) throw new IllegalArgumentException("'projectName' cannot contains any '/'");
		return new File("src/functionalTest/resources/project.d/"+projectName+"/"+resourcePath);
	}
}