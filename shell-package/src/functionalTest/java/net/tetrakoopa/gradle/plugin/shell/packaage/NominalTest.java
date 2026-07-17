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


        // assertEquals("Package file name is correct", dispenserFile("NominalTest---simplestPackage.sh"), "This is me\n");
        assertTrue("Dispenser file has correct name", dispenserFile("NominalTest---simplestPackage.sh").exists());
        assertTrue("Variable 'mdu_sp_package_name' holds correct package name", grepVariableInDispenser("package_name","NominalTest---simplestPackage"));
        assertTrue("Variable 'mdu_sp_package_label' holds correct package label", grepVariableInDispenser("package_label","NominalTest---simplestPackage"));
        assertEquals("There is no 'resource/banner.txt'", false, explodedFileExists("resource/banner.txt"));
    }

    @Test
    public void simplePackageWithFileAtTheRoot() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");
        copyProjectFile("foobar-project", "readme.md");

        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }
            
        shell_package {
            source {
                from ("readme.md") {
                    into "."
                }
                from ("script") {
                    include "**/*.sh"
                    into "bin"
                }
            }
        }
        """);

        buildWithArguments("dispenser");


        // assertEquals("Package file name is correct", dispenserFile("NominalTest---simplestPackage.sh"), "This is me\n");
        assertTrue("Dispenser file has correct name", dispenserFile("NominalTest---simplePackageWithFileAtTheRoot.sh").exists());
        assertTrue("Variable 'mdu_sp_package_name' holds correct package name", grepVariableInDispenser("package_name","NominalTest---simplePackageWithFileAtTheRoot"));
        assertTrue("Variable 'mdu_sp_package_label' holds correct package label", grepVariableInDispenser("package_label","NominalTest---simplePackageWithFileAtTheRoot"));
        assertEquals("There is no 'resource/banner.txt'", false, explodedFileExists("resource/banner.txt"));
        assertEquals("Content folder contains a 'bin/bar.sh'", true, explodedFileExists("content/bin/bar.sh"));
        assertEquals("Content folder contains a 'bin/foo.sh'", true, explodedFileExists("content/bin/foo.sh"));
        assertEquals("Content folder contains a 'bin/reactor.sh'", true, explodedFileExists("content/bin/reactor.sh"));
        assertEquals("Content folder contains a 'readme.md'", true, explodedFileExists("content/readme.md"));
    }


    @Test
    public void simplePackageWithSeveralSubDirectories() throws IOException {


        copyProjectDirectory("foobar-project", "thingies");
        copyProjectFile("foobar-project", "readme.md");

        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }
            
        shell_package {
            source {
                from ("readme.md") {
                    into "."
                }
                from ("thingies") {
                    into "thingies"
                }
            }
        }
        """);

        buildWithArguments("dispenser");


        // assertEquals("Package file name is correct", dispenserFile("NominalTest---simplestPackage.sh"), "This is me\n");
        assertTrue("Dispenser file has correct name", dispenserFile("NominalTest---simplePackageWithSeveralSubDirectories.sh").exists());
        assertTrue("Variable 'mdu_sp_package_name' holds correct package name", grepVariableInDispenser("package_name","NominalTest---simplePackageWithSeveralSubDirectories"));
        assertTrue("Variable 'mdu_sp_package_label' holds correct package label", grepVariableInDispenser("package_label","NominalTest---simplePackageWithSeveralSubDirectories"));
        assertEquals("There is no 'resource/banner.txt'", false, explodedFileExists("resource/banner.txt"));
        assertEquals("Content folder contains a 'thingies/blue/lac.txt'", true, explodedFileExists("content/thingies/blue/lac.txt"));
        assertEquals("Content folder contains a 'thingies/blue/komodo-island-viper.txt'", true, explodedFileExists("content/thingies/blue/komodo-island-viper.txt"));
        assertEquals("Content folder contains a 'thingies/green/frog.txt'", true, explodedFileExists("content/thingies/green/frog.txt"));
        assertEquals("Content folder contains a 'thingies/green/lantern.txt'", true, explodedFileExists("content/thingies/green/lantern.txt"));
        assertEquals("Content folder contains a 'thingies/green/grass.txt'", true, explodedFileExists("content/thingies/green/grass.txt"));
        assertEquals("Content folder contains a 'readme.md'", true, explodedFileExists("content/readme.md"));
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


        assertTrue("Dispenser file has correct name", dispenserFile("foobar.sh").exists());
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


        assertTrue("Dispenser file has correct name", dispenserFile("foobar-1.2.3-buggy-prealpha.sh").exists());
        assertTrue("Variable 'mdu_sp_package_name' holds correct package name", grepVariableInDispenser("package_name","foobar"));
        assertTrue("Variable 'mdu_sp_package_label' holds correct package label", grepVariableInDispenser("package_label","foobar"));
        assertTrue("Variable 'mdu_sp_package_version' holds correct package version", grepVariableInDispenser("package_version","1.2.3-buggy-prealpha"));
        assertEquals("There is no 'resource/banner.txt'", false, explodedFileExists("resource/banner.txt"));
    }


}
