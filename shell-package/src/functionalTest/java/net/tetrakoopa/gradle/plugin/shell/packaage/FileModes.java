package net.tetrakoopa.gradle.plugin.shell.packaage;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

import net.tetrakoopa.gradle.plugin.shell.AbstractShellPackagePluginFunctionalTest;

public class FileModes extends AbstractShellPackagePluginFunctionalTest {

	private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

	@Test
	public void generatedScriptsKeepTheirModeInTheArchive() throws IOException {

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

		final String dispenser = dispenserTextContent("foobar.sh");

		assertTrue("The archive makes the dispenser executable", dispenser.contains("""
				chmod 754 "${MDU_SD_INSTALL_TEMP_DIR}/dispense.sh" || {
				"""));
		assertTrue("The archive makes the launcher reactor executable", dispenser.contains("""
				chmod 744 "${MDU_SD_INSTALL_TEMP_DIR}/content/bin/reactor.sh" || {
				"""));
		assertTrue("The archive leaves user content with its default mode", dispenser.contains("""
				chmod 644 "${MDU_SD_INSTALL_TEMP_DIR}/content/bin/bar.sh" || {
				"""));

		if (WINDOWS) {
			final File registry = new File(buildDir(), "shell/dispenser/file-modes.txt");
			assertTrue("The mode registry exists in the build directory", registry.isFile());
			final String registryContent = new String(Files.readAllBytes(registry.toPath()), StandardCharsets.UTF_8);
			assertTrue("The registry records the dispenser mode", registryContent.contains("754 ./dispense.sh\n"));
			assertTrue("The registry records the launcher reactor mode", registryContent.contains("744 ./content/bin/reactor.sh\n"));
		}
	}

}