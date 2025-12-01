package net.tetrakoopa.gradle.plugin.shell;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import net.tetrakoopa.gradle.plugin.common.IOUtil;


public class ShellPackagePluginFunctionalTest extends AbstractShellPackagePluginFunctionalTest {

    @Rule
    public TestName name = new TestName();

    @Before
    public void init() throws IOException {
        testBuildDir = new File(buildsDir, name.getMethodName());
        Files.createDirectories(buildsDir.toPath());
    }

    @Test
    public void simplePackage() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");
        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }
            
        shell_package {
            name = "foobar"
            source {
                from ("script") {
                    include "**/*.sh"
                    into "bin"
                }
            }
        }
        """);

        final BuildResult result;
        IOUtil.deleteDirectory(testBuildDir, false);
        IOUtil.deleteDirectory(buildDir, false);
        try {

            Files.createDirectories(buildDir.toPath());
            result = GradleRunner.create()
                .withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("dispenser")
                .withProjectDir(projectDir)
                .build();

        } finally {
            IOUtil.moveDirectory(buildDir, testBuildDir);
        }

        assertEquals("There is no 'resource/banner.txt'", false, explodedFileExists("resource/banner.txt"));
        // assertTrue(result.getOutput().contains("Hello from plugin 'shell'"));
    }

    @Test
    public void addBanner() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");
        copyProjectFile("foobar-project", "banner.txt", "banner.txt");
        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }
            
        shell_package {
            name = "foobar"
            source {
                from ("script") {
                    include "**/*.sh"
                    into "bin"
                }
            }
            banner {
                source "banner.txt"
            }
        }
        """);

        final BuildResult result;
        IOUtil.deleteDirectory(testBuildDir, false);
        IOUtil.deleteDirectory(buildDir, false);
        try {

            Files.createDirectories(buildDir.toPath());
            result = GradleRunner.create()
                .withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("dispenser")
                .withProjectDir(projectDir)
                .build();

        } finally {
            IOUtil.moveDirectory(buildDir, testBuildDir);
        }

        assertEquals(explodedTextContent("resource/banner.txt"), "Hello, it's me the {{something}} banner.\n");

        // assertTrue(result.getOutput().contains("Hello from plugin 'shell'"));
    }

    @Test
    public void addBannerWithModification() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");
        copyProjectFile("foobar-project", "banner.txt", "banner.txt");
        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }
            
        shell_package {
            name = "foobar"
            source {
                from ("script") {
                    include "**/*.sh"
                    into "bin"
                }
            }
            banner {
                source "banner.txt"
                modify { line -> line.replace("{{something}}", "nice") }
            }
        }
        """);

        final BuildResult result;
        IOUtil.deleteDirectory(testBuildDir, false);
        IOUtil.deleteDirectory(buildDir, false);
        try {

            Files.createDirectories(buildDir.toPath());
            result = GradleRunner.create()
                .withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("dispenser")
                .withProjectDir(projectDir)
                .build();

        } finally {
            IOUtil.moveDirectory(buildDir, testBuildDir);
        }

        assertEquals(explodedTextContent("resource/banner.txt"), "Hello, it's me the nice banner.\n");

        // assertTrue(result.getOutput().contains("Hello from plugin 'shell'"));
    }
}
