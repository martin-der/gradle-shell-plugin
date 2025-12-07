package net.tetrakoopa.gradle.plugin.shell.packaage;

import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;


public class Banner extends AbstractShellPackagePluginFunctionalTest{
	

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

}
