package net.tetrakoopa.gradle.plugin.shell.packaage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;

public class Readme extends AbstractShellPackagePluginFunctionalTest{

	@Test
    public void addReadmeSimpleLocation() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");
        copyProjectFile("foobar-project", "readme.md", "readme.md");

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
            installer {
                readme "readme.md"
            }
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'mdu_sp_show_readme' holds correct script path", grepVariableInDispenser("show_readme",1));
        assertEquals(explodedTextContent("resource/readme.txt"), """
                # Foobar

                A great projet

                ## How

                It's `magic`.

                ## What

                It's more like {{what}} !
                """);

    }

    @Test
    public void addReadme() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");
        copyProjectFile("foobar-project", "readme.md", "readme.md");

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
            installer {
                readme {
                    location "readme.md"
                }
            }
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'mdu_sp_show_readme' holds correct script path", grepVariableInDispenser("show_readme",1));
        assertEquals(explodedTextContent("resource/readme.txt"), """
                # Foobar

                A great projet

                ## How

                It's `magic`.

                ## What

                It's more like {{what}} !
                """);

    }

    @Test
    public void addReadmeWithModification() throws IOException {


        copyProjectDirectory("foobar-project", "script", "script");
        copyProjectFile("foobar-project", "readme.md", "readme.md");

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
            installer {
                readme {
                    location "readme.md"
                    modify {
                        line -> line.replace("{{what}}", "no magic at all but pure luck")
                    }
                }
            }
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'mdu_sp_show_readme' holds correct script path", grepVariableInDispenser("show_readme",1));
        assertEquals(explodedTextContent("resource/readme.txt"), """
                # Foobar

                A great projet

                ## How

                It's `magic`.

                ## What

                It's more like no magic at all but pure luck !
                """);

    }

}
