package net.tetrakoopa.gradle.plugin.shell.packaage;

import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;


public class TemporaryDirectory extends AbstractShellPackagePluginFunctionalTest{
	

    @Test
    public void usePersistentTempDirectoryByDefault() throws IOException {


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
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'MDU_SD_PERSISTENT_TEMP_FOLDER' holds correct package name by default", rgrepVariableInMainScript("PERSISTENT_TEMP_FOLDER","\"mdu-shell-dispenser__foobar__.*\""));
    }

    @Test
    public void reuseExistingPersistentTempDirectory() throws IOException, InterruptedException {
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
            launcher {
                script "bin/reactor.sh"
            }
        }
        """);

        buildWithArguments("dispenser");

        final ExecutorAndResult firstExecution = executeLauncherMocked("foobar.sh", "launch");
        assertEquals("first launcher execution succeeded", 0, firstExecution.result);
        assertEquals("first launcher execution has no stderr output", "", firstExecution.executor.grabbedError());

        final ExecutorAndResult secondExecution = executeLauncherMocked("foobar.sh", "launch");
        assertEquals("reused temporary directory execution succeeded", 0, secondExecution.result);
        assertEquals("reused temporary directory execution has no stderr output", "", secondExecution.executor.grabbedError());
    }

    @Test
    public void withoutPersistentTempDirectory () throws IOException {


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
            keepTemporaryDirectory = false
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'MDU_SD_PERSISTENT_TEMP_FOLDER' is not declared", noGrepVariableInMainScript("PERSISTENT_TEMP_FOLDER"));
    }

}
