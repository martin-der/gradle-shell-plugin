package net.tetrakoopa.gradle.plugin.shell.packaage;

import org.junit.Test;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;



public class NominalTest extends AbstractShellPackagePluginFunctionalTest {

    @Test
    public void simplestPackage() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");

        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }
            
        shell_package {
            source {
                from ("script") {
                    include "**/*.sh"
                    into "bin"
                }
            }
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'mdu_sp_package_name' holds correct package name", grepVariableInDispenser("package_name","NominalTest---simplestPackage"));
        assertTrue("Variable 'mdu_sp_package_label' holds correct package label", grepVariableInDispenser("package_label","NominalTest---simplestPackage"));
        assertEquals("There is no 'resource/banner.txt'", false, explodedFileExists("resource/banner.txt"));
    }

    @Test
    public void simplePackageWithNameAndLabel() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");

        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }
            
        shell_package {
            name = "foobar"
            label = "Foo Bar Frenzy"
            source {
                from ("script") {
                    include "**/*.sh"
                    into "bin"
                }
            }
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'mdu_sp_package_name' holds correct package name", grepVariableInDispenser("package_name","foobar"));
        assertTrue("Variable 'mdu_sp_package_label' holds correct package label", grepVariableInDispenser("package_label","Foo Bar Frenzy"));
        assertEquals("There is no 'resource/banner.txt'", false, explodedFileExists("resource/banner.txt"));
    }

        @Test
    public void simplePackageWithNameAndVersion() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");

        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }
            
        shell_package {
            name = "foobar"
            version = "1.2.3-buggy-prealpha"
            source {
                from ("script") {
                    include "**/*.sh"
                    into "bin"
                }
            }
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'mdu_sp_package_name' holds correct package name", grepVariableInDispenser("package_name","foobar"));
        assertTrue("Variable 'mdu_sp_package_label' holds correct package label", grepVariableInDispenser("package_label","foobar"));
        assertTrue("Variable 'mdu_sp_package_version' holds correct package version", grepVariableInDispenser("package_version","1.2.3-buggy-prealpha"));
        assertEquals("There is no 'resource/banner.txt'", false, explodedFileExists("resource/banner.txt"));
    }


}
