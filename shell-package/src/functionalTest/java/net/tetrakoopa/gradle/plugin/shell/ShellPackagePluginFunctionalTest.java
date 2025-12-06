package net.tetrakoopa.gradle.plugin.shell;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;



public class ShellPackagePluginFunctionalTest extends AbstractShellPackagePluginFunctionalTest {

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


        assertTrue("Variable 'mdu_sp_package_name' holds correct package name", grepVariableInDispenser("package_name","ShellPackagePluginFunctionalTest_$_simplestPackage"));
        assertTrue("Variable 'mdu_sp_package_label' holds correct package label", grepVariableInDispenser("package_label","ShellPackagePluginFunctionalTest_$_simplestPackage"));
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


        assertTrue("Variable 'mdu_sp_package_name' holds correct package name", grepVariableInDispenser("package_name","foobar"));
        assertEquals(explodedTextContent("resource/banner.txt"), "Hello, it's me the {{something}} banner.\n");
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
