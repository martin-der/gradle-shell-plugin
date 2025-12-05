package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import net.tetrakoopa.gradle.plugin.common.IOUtil;


public class AbstractShellPackagePluginFunctionalTest {

	protected static final File projectsDir = new File("build/functionalTest/tests");

	protected File projectDir;
    protected File buildDir;

	protected TestData testData;

    protected static class TestData {
        String dispenser_sh;
    }

    protected BuildResult buildWithArguments(String ... arguments) {
        return GradleRunner.create()
            .withDebug(true)
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(arguments)
            .withProjectDir(projectDir)
            .build();
    }

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
       return new File(buildDir, "shell/dispenser/exploded/"+filePath);
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

	protected boolean grepVariableInDispenser(String variable_name_suffix, int value) throws IOException {
        return grepVariableInDispenser(variable_name_suffix, String.valueOf(value), "i");
    }
    protected boolean grepVariableInDispenser(String variable_name_suffix, String value) throws IOException {
        return grepVariableInDispenser(variable_name_suffix, value, "");
    }
    protected boolean grepVariableInDispenser(String name_suffix, String value, String typeArg) throws IOException {
        final String dispenser_sh = getDispenseSh();
        System.out.println("Expect '"+"\n"+"declare -r"+typeArg+" mdu_sp_"+name_suffix+"="+value+"\n"+"'");
        return dispenser_sh.contains("\n"+"declare -r"+typeArg+" mdu_sp_"+name_suffix+"="+value+"\n");
    }

    private String getDispenseSh() throws IOException {
        if (testData.dispenser_sh == null) {
            testData.dispenser_sh = explodedTextContent("dispense.sh");
        }
        return testData.dispenser_sh;
    }

	private File projectResource(String projectName, String resourcePath) {
		if (projectName.contains("/")) throw new IllegalArgumentException("'projectName' cannot contains any '/'");
		return new File("src/functionalTest/resources/project.d/"+projectName+"/"+resourcePath);
	}
}