package net.tetrakoopa.gradle.plugin.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ShellPackagePluginTest {

	@Test
	public void leavesAnOrdinaryValueExactlyAsTheEscaperWouldRenderIt() {
		assertEquals("Hobbes", ShellPackagePlugin.renderShellValue("Hobbes"));
		assertEquals("1.2.3-buggy-prealpha", ShellPackagePlugin.renderShellValue("1.2.3-buggy-prealpha"));
		assertEquals("'Foo Bar'", ShellPackagePlugin.renderShellValue("Foo Bar"));
		assertEquals("'; rm -rf /'", ShellPackagePlugin.renderShellValue("; rm -rf /"));
		// Single quotes, not backslashes, are what make '$' inert here.
		assertEquals("'$(rm -rf /)'", ShellPackagePlugin.renderShellValue("$(rm -rf /)"));
	}

	@Test
	public void keepsTheContentDirectoryReferenceLive() {
		assertEquals("\"${MDU_SD_DISPENSER_CONTENT_DIRECTORY}\"",
			ShellPackagePlugin.renderShellValue("{{MDU-SD_CONTENT-DIRECTORY}}"));
	}

	@Test
	public void concatenatesQuotedSegmentsAroundTheReference() {
		// 'safe' characters stay unquoted, and bash still concatenates the adjacent words into one
		assertEquals("/opt\"${MDU_SD_DISPENSER_CONTENT_DIRECTORY}\"/bin",
			ShellPackagePlugin.renderShellValue("/opt{{MDU-SD_CONTENT-DIRECTORY}}/bin"));
		assertEquals("'my app'"+"\"${MDU_SD_DISPENSER_CONTENT_DIRECTORY}\"",
			ShellPackagePlugin.renderShellValue("my app{{MDU-SD_CONTENT-DIRECTORY}}"));
	}

	@Test
	public void handlesRepeatedPlaceholders() {
		assertEquals("\"${MDU_SD_DISPENSER_CONTENT_DIRECTORY}\""+"-x"+"\"${MDU_SD_DISPENSER_CONTENT_DIRECTORY}\"",
			ShellPackagePlugin.renderShellValue("{{MDU-SD_CONTENT-DIRECTORY}}-x{{MDU-SD_CONTENT-DIRECTORY}}"));
	}

	@Test
	public void passesNullThroughAndRendersEmptyAsAnEmptyAssignment() {
		assertNull(ShellPackagePlugin.renderShellValue(null));
		// 'key=' already assigns the empty string, so there is nothing to quote.
		assertEquals("", ShellPackagePlugin.renderShellValue(""));
	}
}
