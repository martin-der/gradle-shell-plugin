package net.tetrakoopa.gradle.plugin.shell.packaage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
     * An install is not a launch, and does not leave the package half-prepared for one: having done
     * its job it takes its own directory back, so the next launch starts from nothing just as the
     * very first one did.
     *
     * <p>Getting there means actually completing an install. An install that is interrupted partway
     * keeps its directory, so it would leave the following launch reporting 0 -- which says nothing
     * about a user who installs normally.
     */
    @Test
    public void aLaunchAfterAnInstallIsAFirstLaunch() throws IOException, InterruptedException {
        createProjectWithLauncher();

        buildWithArguments("dispenser");

        final File installTarget = new File(buildDir(), "installed");
        final ExecutorAndResult install = executeLauncherMocked(
            answering("3\n" + installTarget.getAbsolutePath() + "\n"), "foobar.sh", "install");
        assertEquals("the install completed", 0, install.result);
        assertTrue("the launcher was installed",
            new File(installTarget, "launcher.sh").isFile());

        assertEquals("and the package took its own directory back", 0, persistentTempDirectories().length);

        assertEquals("so the launch that follows an install is a first launch", "1", launchAndReadFlag());
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

    /**
     * The directory the package extracts itself into, which is also what it uses to decide whether
     * an earlier run has been here.
     */
    private File[] persistentTempDirectories() {
        final File[] candidates = new File(buildDir(), "root/tmp").listFiles(
            (dir, name) -> name.startsWith("mdu-shell-dispenser__foobar__") && new File(dir, name).isDirectory());
        return candidates == null ? new File[0] : candidates;
    }

    /**
     * Answers the installer's prompts. The third location offered is "a custom location...", which
     * takes the next line as a directory -- one inside this test's own build directory, rather than
     * the real {@code $HOME/bin} the earlier choices point at.
     */
    private static Consumer<OutputStream> answering(String answers) {
        return stdin -> {
            try {
                stdin.write(answers.getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}