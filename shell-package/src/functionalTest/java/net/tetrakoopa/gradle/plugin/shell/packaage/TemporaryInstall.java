package net.tetrakoopa.gradle.plugin.shell.packaage;

import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;


public class TemporaryInstall extends AbstractShellPackagePluginFunctionalTest{
	

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
