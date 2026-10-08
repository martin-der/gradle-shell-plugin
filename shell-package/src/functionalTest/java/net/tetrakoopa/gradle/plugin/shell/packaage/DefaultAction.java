package net.tetrakoopa.gradle.plugin.shell.packaage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.function.Consumer;

import org.gradle.testkit.runner.BuildResult;
import org.junit.Assume;
import org.junit.Test;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;

/**
 * {@code shell_package > action > default} : the action the generated package performs when its
 * first parameter names none.
 */
public class DefaultAction extends AbstractShellPackagePluginFunctionalTest {

    private static final String MISSING_DEFAULT_WARNING = "no 'default' is set";

    @Test
    public void aLaunchDefaultIsWrittenIntoTheDispenser() throws IOException {
        createProjectWithLauncher("""
            action {
                defaultAction 'launch'
            }
            """);

        final BuildResult result = buildWithArguments("dispenser");

        assertTrue("Variable 'mdu_sp_default_action' holds the default action",
            grepVariableInDispenser("default_action", "launch"));
        assertFalse("a default that is set needs no warning", result.getOutput().contains(MISSING_DEFAULT_WARNING));
    }

    @Test
    public void theNamedArgumentShorthandSetsTheDefault() throws IOException {
        // 'default' is a reserved word in Groovy, so this is how the option keeps its name.
        createProjectWithLauncher("action(default: 'install')");

        final BuildResult result = buildWithArguments("dispenser");

        assertTrue("Variable 'mdu_sp_default_action' holds the default action",
            grepVariableInDispenser("default_action", "install"));
        assertFalse("a default that is set needs no warning", result.getOutput().contains(MISSING_DEFAULT_WARNING));
    }

    @Test
    public void anActionBlockWithoutADefaultWarns() throws IOException {
        createProjectWithLauncher("""
            action {
                mode = 'action-mode-prefix'
            }
            """);

        final BuildResult result = buildWithArguments("dispenser");

        assertTrue("the missing default is reported :\n"+result.getOutput(),
            result.getOutput().contains(MISSING_DEFAULT_WARNING));
    }

    @Test
    public void aDefaultThatIsNeitherLaunchNorInstallFailsTheBuild() throws IOException {
        createProjectWithLauncher("""
            action {
                defaultAction 'restart'
            }
            """);

        final BuildResult result = buildWithArgumentsAndFail("dispenser");

        assertTrue("the build says which values are allowed :\n"+result.getOutput(),
            result.getOutput().contains("expected 'launch' or 'install'"));
    }

    @Test
    public void aLaunchDefaultWithoutALauncherFailsTheBuild() throws IOException {
        createProjectWithoutLauncher("""
            action {
                defaultAction 'launch'
            }
            """);

        final BuildResult result = buildWithArgumentsAndFail("dispenser");

        assertTrue("the build says there would be nothing to launch :\n"+result.getOutput(),
            result.getOutput().contains("without a 'launcher' block"));
    }

    @Test
    public void theArchiveLaunchesWithoutAFirstParameter() throws IOException, InterruptedException {
        assumeShellScriptsAreExecutable();
        createProjectWithLauncher("""
            action {
                defaultAction 'launch'
            }
            """);
        buildWithArguments("dispenser");

        final ExecutorAndResult execution = executeLauncherMocked("foobar.sh");

        assertEquals("the package launched without being told to", 0, execution.result);
        assertEquals("and ran the launcher script", """
                There was a tiger named 'some-unknown-tiger'
                and a dog named 'some-unknown-dog'.
                """,
            execution.executor.grabbedOutput());
    }

    @Test
    public void anExplicitInstallBeatsTheLaunchDefault() throws IOException, InterruptedException {
        assumeShellScriptsAreExecutable();
        createProjectWithLauncher("""
            action {
                defaultAction 'launch'
            }
            """);
        buildWithArguments("dispenser");

        // Installing is interactive. Reading the answer hits end of input, which is all this needs :
        // by then the install path has been taken, and the default has not hijacked the run.
        final ExecutorAndResult execution = executeLauncherMocked(closingStdin(), "foobar.sh", "install");
        final String output = execution.executor.grabbedOutput();

        assertTrue("the installer ran :\n"+output, output.contains("Installation of 'foobar'"));
        assertFalse("the launcher did not run instead", output.contains("There was a tiger"));
    }

    private void createProjectWithLauncher(String actionDsl) throws IOException {
        createProject(actionDsl, """
            launcher {
                script "bin/reactor.sh"
            }
            """);
    }

    private void createProjectWithoutLauncher(String actionDsl) throws IOException {
        createProject(actionDsl, "");
    }

    private void createProject(String actionDsl, String launcherDsl) throws IOException {
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
            %s%s
        }
        """.formatted(launcherDsl, actionDsl));
    }

    /**
     * The harness runs the generated package by executing it, which needs a POSIX shell. Windows
     * cannot execute a {@code .sh} file at all, so those assertions are skipped there — the
     * build-side ones above still run everywhere.
     */
    private static void assumeShellScriptsAreExecutable() {
        Assume.assumeFalse("the test setup executes shell scripts, which Windows cannot do",
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows"));
    }

    /** An executor whose stdin is immediately at end of file. */
    private static Consumer<OutputStream> closingStdin() {
        return stdin -> {
            try {
                stdin.close();
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        };
    }
}
