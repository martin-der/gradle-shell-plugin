package net.tetrakoopa.gradle.plugin.shell.packaage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;

public class Launcher extends AbstractShellPackagePluginFunctionalTest {

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
