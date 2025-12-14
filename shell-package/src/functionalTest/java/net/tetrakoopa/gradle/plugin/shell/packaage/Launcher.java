package net.tetrakoopa.gradle.plugin.shell.packaage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;

import org.gradle.testkit.runner.BuildResult;
import org.junit.Test;
import net.tetrakoopa.gradle.plugin.common.SystemUtil;
import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;

public class Launcher extends AbstractShellPackagePluginFunctionalTest {

    @Test
    public void addLauncher() throws IOException {


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


        assertTrue("Variable 'mdu_sp_executable_reactor_script' holds correct script path", grepVariableInDispenser("executable_reactor_script","bin/reactor.sh"));
        assertTrue("Not user properties file is generated", explodedFileDoesNotExist("resource/launcher-properties.sh"));
        assertEquals("Stdout output of the launched script is what's expected", """
                There was a tiger named 'some-unknown-tiger'
                and a dog named 'some-unknown-dog'.
                """,
            getLauncherStdOut("foobar.sh", "launch"));
    }

    @Test
    public void addLauncherWithEnvironmentProperties() throws IOException {


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
                environment [
                    tiger: 'Hobbes',
                    dog: 'Snoopy',
                ]
            }
        }
        """);

        buildWithArguments("dispenser");


        assertTrue("Variable 'mdu_sp_executable_reactor_script' holds correct script path", grepVariableInDispenser("executable_reactor_script","bin/reactor.sh"));
        assertTrue("Variable 'mdu_sp_executable_has_environment_properties' is 0", grepVariableInDispenser("executable_has_environment_properties",1));
        assertEquals(explodedTextContent("resource/launcher-properties.sh"), """
                tiger=Hobbes
                export tiger

                dog=Snoopy
                export dog
                """);
        assertEquals("Stdout output of the launched script is what's expected", """
                There was a tiger named 'Hobbes'
                and a dog named 'Snoopy'.
                """,
            getLauncherStdOut("foobar.sh", "launch"));

    }

    @Test
    public void addLauncherWithMissingReactorScript() throws IOException {
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
                script "bin/reactor_doesn_t_exist.sh"
                environment [
                    tiger: 'Hobbes',
                    dog: 'Snoopy',
                ]
            }
        }
        """);

        final BuildResult result = buildWithArgumentsAndFail("dispenser");

        assertTrue("", result.getOutput().contains("""
                Execution failed for task ':dispenser'.
                > Launcher script 'bin/reactor_doesn_t_exist.sh' does not exist
                """));

    }


    private String getLauncherStdOut(String packageFileName, String... arguments) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        new SystemUtil.ProcessExecutor(concat(dispenserFile(packageFileName).getAbsolutePath(), arguments))
            .output(output)
            .run();
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    @SafeVarargs
    public static final <T> T[] concat(T[] t1, T... t2) {
        @SuppressWarnings("unchecked")
        final T[] f = (T[])Array.newInstance(t1.getClass().getComponentType(), t1.length + t2.length);
        System.arraycopy(t1, 0, f, 0, t1.length);
        System.arraycopy(t2, 0, f, t1.length, t2.length);
        return f;
    }
    @SafeVarargs
    public static final <T> T[] concat(T t, T... t2) {
        @SuppressWarnings("unchecked")
        final T[] f = (T[])Array.newInstance(t2.getClass().getComponentType(), 1 + t2.length);
        f[0] = t;
        System.arraycopy(t2, 0, f, 1, t2.length);
        return f;
    }
	
}
