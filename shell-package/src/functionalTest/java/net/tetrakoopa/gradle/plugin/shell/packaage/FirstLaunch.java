package net.tetrakoopa.gradle.plugin.shell.packaage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;

/**
 * MDU_SD_FIRST_LAUNCH tells a launched application whether this is the run that had to set the
 * package up, as opposed to a run that found the persistent temp directory already in place.
 */
public class FirstLaunch extends AbstractShellPackagePluginFunctionalTest {

    private static final String LAUNCHER_ECHOING_THE_FIRST_LAUNCH_FLAG =
        "echo \"${MDU_SD_FIRST_LAUNCH}\"\n";

    @Test
    public void theFirstLaunchIsOneAndLaterLaunchesAreZero() throws IOException, InterruptedException {
        createProjectWithLauncher();

        buildWithArguments("dispenser");

        assertEquals("the first launch runs", "1", launchAndReadFlag());
        assertEquals("the second launch runs", "0", launchAndReadFlag());
        assertEquals("and so does every launch after that", "0", launchAndReadFlag());
    }

    /**
     * The flag reports on the persistent temp directory, not on launches specifically: installing is
     * what brings that directory into existence, so a launch following an install is a launch onto
     * an already prepared package.
     */
    @Test
    public void aLaunchAfterAnInstallIsNotAFirstLaunch() throws IOException, InterruptedException {
        createProjectWithLauncher();

        buildWithArguments("dispenser");

        // Installing is interactive and this only needs the directory to have been created, which the
        // package does before the installer ever prompts. Hitting end of input aborts the installer.
        executeLauncherMocked(closingStdin(), "foobar.sh", "install");

        assertEquals("the launch that follows an install is not a first launch", "0", launchAndReadFlag());
    }

    /**
     * Nothing is carried over between runs without a persistent temp directory, so the application
     * cannot tell one run from the next and every run has to look like a first one.
     */
    @Test
    public void withoutAPersistentTempDirectoryEveryLaunchIsAFirstLaunch() throws IOException, InterruptedException {
        createProjectWithLauncher("    keepTemporaryDirectory = false\n");

        buildWithArguments("dispenser");

        assertEquals("the first launch runs", "1", launchAndReadFlag());
        assertEquals("and so does the second", "1", launchAndReadFlag());
    }

    /**
     * The launcher is not the script that decides this, so the variable has to cross into it
     * through the environment rather than as a local of some shell function.
     */
    @Test
    public void theFlagIsExportedToTheEnvironment() throws IOException {
        createProjectWithLauncher();

        buildWithArguments("dispenser");

        assertTrue("the generated package exports MDU_SD_FIRST_LAUNCH",
            dispenserTextContent("foobar.sh").contains("export MDU_SD_FIRST_LAUNCH\n"));
    }

    private String launchAndReadFlag() throws IOException, InterruptedException {
        final ExecutorAndResult execution = executeLauncherMocked("foobar.sh", "launch");
        assertEquals("launcher ran", 0, execution.result);
        assertEquals("launcher reported no error", "", execution.executor.grabbedError());
        return execution.executor.grabbedOutput().trim();
    }

    /**
     * A first-launch flag is only as good as the directory it keys off, so these tests rely on the
     * persistent temp directory being on, as it is by default.
     */
    private void createProjectWithLauncher() throws IOException {
        createProjectWithLauncher("");
    }

    /**
     * @param extraConfiguration further {@code shell_package} lines -- indented and newline
     *                           terminated -- placed between the package name and its source
     */
    private void createProjectWithLauncher(String extraConfiguration) throws IOException {
        createProjectFile("launcher.sh", LAUNCHER_ECHOING_THE_FIRST_LAUNCH_FLAG);
        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }

        shell_package {
            name = "foobar"
        %s    source {
                from ("launcher.sh") {
                    into "."
                }
            }
            launcher {
                script "launcher.sh"
            }
        }
        """.formatted(extraConfiguration));
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