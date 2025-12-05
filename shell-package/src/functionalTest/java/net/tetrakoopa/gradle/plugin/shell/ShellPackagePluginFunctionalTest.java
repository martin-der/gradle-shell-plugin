package net.tetrakoopa.gradle.plugin.shell;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;



public class ShellPackagePluginFunctionalTest extends AbstractShellPackagePluginFunctionalTest {

    @Rule
    public TestName name = new TestName();

    @Before
    public void init() throws IOException {
        projectDir = new File(projectsDir, this.getClass().getSimpleName()+"_$_"+name.getMethodName());
        buildDir = new File(projectDir, "build");
        Files.createDirectories(projectsDir.toPath());
        testData = new TestData();
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

        buildWithArguments("dispenser");


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

        buildWithArguments("dispenser");


        assertEquals(explodedTextContent("resource/banner.txt"), "Hello, it's me the {{something}} banner.\n");

        // assertTrue(result.getOutput().contains("Hello from plugin 'shell'"));
    }

    @Test
    public void addBannerWithSubstitutions() throws IOException {


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

        buildWithArguments("dispenser");


        assertEquals(explodedTextContent("resource/banner.txt"), "Hello, it's me the nice banner.\n");

        // assertTrue(result.getOutput().contains("Hello from plugin 'shell'"));
    }

    @Test
    public void addLauncher() throws IOException {


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
            launcher {
                script "bin/youkikakou"
            }
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'mdu_sp_executable_reactor_script' holds correct script path", grepVariableInDispenser("executable_reactor_script","bin/youkikakou"));
        assertTrue("Variable 'mdu_sp_executable_has_environment_properties' is 0", grepVariableInDispenser("executable_has_environment_properties",0));

    }

    @Test
    public void addLauncherWithEnvironmentProperties() throws IOException {


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
            launcher {
                script "bin/youkikakou"
                environment [
                    tiger: 'Hobbes',
                    dog: 'Snoopy',
                ]
            }
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'mdu_sp_executable_reactor_script' holds correct script path", grepVariableInDispenser("executable_reactor_script","bin/youkikakou"));
        assertTrue("Variable 'mdu_sp_executable_has_environment_properties' is 0", grepVariableInDispenser("executable_has_environment_properties",1));
        assertEquals(explodedTextContent("resource/launcher-properties.sh"), """
                tiger=Hobbes
                dog=Snoopy
                """);

    }

}
