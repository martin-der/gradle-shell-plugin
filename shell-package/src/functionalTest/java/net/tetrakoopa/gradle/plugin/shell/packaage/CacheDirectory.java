package net.tetrakoopa.gradle.plugin.shell.packaage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;

/**
 * MDU_SD_CACHE_DIRECTORY is the launcher's own scratch space, created next to the content the
 * package extracted for itself.
 */
public class CacheDirectory extends AbstractShellPackagePluginFunctionalTest {

    private static final String LAUNCHER_ECHOING_THE_CACHE_DIRECTORY =
        "echo \"${MDU_SD_CACHE_DIRECTORY}\"\n";

    @Test
    public void cacheDirectoryIsProvidedToTheLauncher() throws IOException, InterruptedException {
        createProjectWithLauncher(LAUNCHER_ECHOING_THE_CACHE_DIRECTORY);

        buildWithArguments("dispenser");

        final ExecutorAndResult execution = executeLauncherMocked("foobar.sh", "launch");
        assertEquals("launcher ran", 0, execution.result);
        assertEquals("launcher reported no error", "", execution.executor.grabbedError());

        final File cache = persistentTempDirectory().toPath().resolve("user_cache").toFile();
        assertTrue("MDU_SD_CACHE_DIRECTORY points at the created directory ('" + cache.getAbsolutePath() + "')",
            execution.executor.grabbedOutput().trim().equals(cache.getAbsolutePath()));
        assertTrue("the cache directory exists", cache.isDirectory());
    }

    @Test
    public void cacheDirectoryIsNotProvidedToTheInstaller() throws IOException, InterruptedException {
        createProjectWithLauncher(LAUNCHER_ECHOING_THE_CACHE_DIRECTORY);

        buildWithArguments("dispenser");

        // Installing is interactive. Reading the answer hits end of input, so the installer gives up
        // before it copies anything anywhere -- which is all this needs: by then it is well past the
        // point where a cache directory would have been created for it.
        executeLauncherMocked(closingStdin(), "foobar.sh", "install");

        assertFalse("no cache directory is created for the installer", userCache().exists());
        assertFalse("and the cache directory is only prepared on the launch path",
            cacheDirectoryIsPreparedOutsideTheLaunchBranch());
    }

    @Test
    public void cacheDirectoryIsCreatedOnlyOnceSoItsContentSurvives() throws IOException, InterruptedException {
        createProjectWithLauncher(LAUNCHER_ECHOING_THE_CACHE_DIRECTORY);

        buildWithArguments("dispenser");

        assertEquals("first launch ran", 0, executeLauncherMocked("foobar.sh", "launch").result);

        // Something the application put there, standing in for a real cache.
        final File cached = userCache().toPath().resolve("session.json").toFile();
        createFile(cached, "{\"kept\":true}\n");
        assertTrue("the cache directory holds what the application wrote", cached.isFile());

        assertEquals("second launch ran", 0, executeLauncherMocked("foobar.sh", "launch").result);

        assertTrue("the cache directory is still there", userCache().isDirectory());
        assertTrue("its content survived the second launch", cached.isFile());
        assertEquals("and is unchanged",
            "{\"kept\":true}\n",
            new String(java.nio.file.Files.readAllBytes(cached.toPath()), StandardCharsets.UTF_8));
    }

    /**
     * A cache directory that survives is the point of the persistent temp directory, so these tests
     * rely on it being on by default; the launcher is useless without a directory it can point at.
     */
    private void createProjectWithLauncher(String launcherScript) throws IOException {
        createProjectFile("launcher.sh", launcherScript);
        createProjectFile("settings.gradle", "");
        createProjectFile("build.gradle",
        """
        plugins {
            id('shell-package')
        }

        shell_package {
            name = "foobar"
            source {
                from ("launcher.sh") {
                    into "."
                }
            }
            launcher {
                script "launcher.sh"
            }
        }
        """);
    }

    private File userCache() {
        return persistentTempDirectory().toPath().resolve("user_cache").toFile();
    }

    /**
     * The dispenser extracts next to the (mocked) system temporary directory, which the test setup
     * points at this package's own build directory.
     */
    private File persistentTempDirectory() {
        final File[] candidates = new File(buildDir(), "root/tmp").listFiles(
            (dir, name) -> name.startsWith("mdu-shell-dispenser__foobar__") && new File(dir, name).isDirectory());
        if (candidates == null || candidates.length != 1) {
            throw new IllegalStateException("Expected exactly one persistent temp directory, got "
                + (candidates == null ? "none" : candidates.length));
        }
        return candidates[0];
    }

    /**
     * Whether anything outside the launch branch calls {@code prepare_user_cache_directory}.
     *
     * <p>Running the installer cannot observe the exported variable directly, so this guards the
     * "launchers only" half of the contract at the call site instead: the function is *defined*
     * above the branch and *called* inside it, so it is the call that has to stay there.
     */
    private boolean cacheDirectoryIsPreparedOutsideTheLaunchBranch() throws IOException {
        final String dispense = explodedTextContent("dispense.sh");
        final int launchBranch = dispense.indexOf("\nif [ ${action} = 'LAUNCH' ]; then");
        final int installerBranch = dispense.indexOf("# |      I N S T A L L      |");
        if (launchBranch < 0 || installerBranch < launchBranch) {
            throw new IllegalStateException("Cannot locate the launch and install branches");
        }

        final Matcher calls = Pattern.compile("(?m)^[ \\t]*prepare_user_cache_directory[ \\t]*$").matcher(dispense);
        int callsFound = 0;
        while (calls.find()) {
            callsFound++;
            final boolean insideLaunchBranch = calls.start() > launchBranch && calls.start() < installerBranch;
            if (!insideLaunchBranch) {
                return true;
            }
        }
        if (callsFound != 1) {
            throw new IllegalStateException("Expected exactly one call to prepare_user_cache_directory, found " + callsFound);
        }
        return false;
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