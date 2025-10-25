package net.tetrakoopa.gradle.plugin.shell;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import net.tetrakoopa.gradle.plugin.common.IOUtil;

import static org.junit.jupiter.api.Assertions.*;


public class ShellPackagePluginFunctionalTest {

    @Test
    public void canRunTask() throws IOException {

        File projectDir = new File("build/functionalTest");
        File buildDir = new File("build/functionalTest/build");
        File buildsDir = new File(projectDir, "builds");
        File testBuildDir = new File(buildsDir, "canRunTask");

        IOUtil.copyDirectory(new File("src/functionalTest/resources/foobar-project/script"), new File(projectDir, "script"));  

        Files.createDirectories(buildsDir.toPath());

        Files.createDirectories(projectDir.toPath());
        writeString(new File(projectDir, "settings.gradle"), "");
        writeString(new File(projectDir, "build.gradle"),
        """
        plugins {
            id('shell-package')
        }
            
        shell_package {
            name = "foobar"
            source {
                from file("script")
            }
        }
        """);

        final BuildResult result;
        IOUtil.deleteDirectory(testBuildDir, false);
        IOUtil.deleteDirectory(buildDir, false);
        try {

            result = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("dispenser")
                .withProjectDir(projectDir)
                .build();
        } finally {
            IOUtil.moveDirectory(buildDir, testBuildDir);
        }



        // assertTrue(result.getOutput().contains("Hello from plugin 'shell'"));
    }

    private void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
}
