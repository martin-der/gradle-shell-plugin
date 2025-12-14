package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import net.tetrakoopa.gradle.plugin.common.IOUtil;


public class AbstractShellPackagePluginFunctionalTest {

	protected static final File projectsDir = new File("build/functionalTest");

	protected File projectDir;
    protected File buildDir;

	protected TestData testData;

    protected static class TestData {
        String dispenser_sh;
    }

    @Rule
    public TestName name = new TestName();

    @Before
    public void init() throws IOException {

        final String[] rootPackage = AbstractShellPackagePluginFunctionalTest.class.getPackageName().split("\\.");
        final String[] thispackage = this.getClass().getPackageName().split("\\.");

        if (!isInSamepackageOrSubPacakge(thispackage, rootPackage)) {
            throw new IllegalStateException("This test must be in the same pacakge or a sub pacakge of "+AbstractShellPackagePluginFunctionalTest.class.getPackageName());
        }

        final String subdirectory = Stream.of(Arrays.copyOfRange(thispackage, rootPackage.length,thispackage.length)).collect(Collectors.joining(File.separator));
        final File parentProjectDir = new File(projectsDir, subdirectory);
        Files.createDirectories(parentProjectDir.toPath());

        projectDir = new File(parentProjectDir, this.getClass().getSimpleName()+"---"+name.getMethodName());
        buildDir = new File(projectDir, "build");
        Files.createDirectories(projectsDir.toPath());
        testData = new TestData();
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
    protected BuildResult buildWithArgumentsAndFail(String ... arguments) {
        return GradleRunner.create()
            .withDebug(true)
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(arguments)
            .withProjectDir(projectDir)
            .buildAndFail();
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
	protected boolean explodedFileDoesNotExist(String filePath) {
       return !explodedFile(filePath).exists();
    }
	protected InputStream explodedInputStream(String filePath) throws FileNotFoundException {
       return new FileInputStream(explodedFile(filePath));
    }
	protected String explodedTextContent(String filePath) throws IOException {
        try (InputStream stream = explodedInputStream(filePath)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected File dispenserFile(String fileName) {
       return new File(buildDir, "shell/dispenser/"+fileName);
    }
	protected InputStream dispenserFileStream(String fileName) throws FileNotFoundException {
       return new FileInputStream(dispenserFile(fileName));
    }
	protected String dispenserTextContent(String fileName) throws IOException {
        try (InputStream stream = dispenserFileStream(fileName)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }


	protected boolean grepVariableInDispenser(String variable_name_suffix, int value) throws IOException {
        return grepVariableInDispenser(variable_name_suffix, String.valueOf(value), "i");
    }
    protected boolean grepVariableInDispenser(String variable_name_suffix, String value) throws IOException {
        return grepVariableInDispenser(variable_name_suffix, value, "");
    }
    protected boolean grepVariableInDispenser(String name_suffix, String value, String typeArg) throws IOException {
        final String dispenser_sh = getDispenseSh();
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


    private static boolean isInSamepackageOrSubPacakge(String[] thispackage, String[] rootPackage) {
        if (!(rootPackage.length <= thispackage.length)) {
            return false;
        }
        for (int i=0; i<rootPackage.length; i++) {
            if (!thispackage[i].equals(rootPackage[i])) {
                return false;
            }
        }

        return true;
    }

}