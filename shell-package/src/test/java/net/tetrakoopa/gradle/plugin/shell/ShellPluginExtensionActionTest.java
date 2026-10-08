package net.tetrakoopa.gradle.plugin.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

import net.tetrakoopa.gradle.plugin.exception.InvalidPluginConfigurationException;

/**
 * {@code shell_package > action > default} : which values the DSL accepts, and which of them the
 * generated package is actually able to honour.
 */
public class ShellPluginExtensionActionTest {

	private ShellPluginExtension extension;

	@Before
	public void newExtension() {
		final Project project = ProjectBuilder.builder().withName("foobar").build();
		extension = project.getExtensions().create(ShellPluginExtension.NAME, ShellPluginExtension.class);
	}

	@Test
	public void aBuildScriptThatNeverMentionsActionIsUntouched() {
		extension.validate();
	}

	@Test
	public void acceptsLaunchAsDefaultWhenThePackageHasALauncher() {
		withLauncher();
		extension.getAction().setDefaultAction(ShellPluginExtension.ACTION_LAUNCH);
		extension.validate();
	}

	@Test
	public void acceptsInstallAsDefaultWithoutAnyLauncher() {
		extension.getAction().setDefaultAction(ShellPluginExtension.ACTION_INSTALL);
		extension.validate();
	}

	@Test
	public void rejectsADefaultThatIsNeitherLaunchNorInstall() {
		extension.getAction().setDefaultAction("restart");
		assertValidateFails("expected 'launch' or 'install'");
	}

	@Test
	public void rejectsLaunchAsDefaultWhenThereIsNothingToLaunch() {
		extension.getAction().setDefaultAction(ShellPluginExtension.ACTION_LAUNCH);
		assertValidateFails("without a 'launcher' block");
	}

	@Test
	public void anActionBlockWithoutADefaultIsOnlyAWarning() {
		// What an 'action { mode = 'action-mode-prefix' }' block boils down to : configured, but
		// with nothing for the generated package to fall back on. It builds, it just says so.
		extension.getAction().setMode(ShellPluginExtension.MultiActionModeStrategy.ACTION_MODE_PREFIX);
		extension.validate();
	}

	@Test
	public void theNamedArgumentShorthandSetsTheDefault() {
		extension.action(Map.of("default", ShellPluginExtension.ACTION_INSTALL));

		assertEquals(ShellPluginExtension.ACTION_INSTALL, extension.getAction().getDefaultAction());
		assertTrue("the shorthand counts as configuring the block", extension.getAction().isConfigured());
		extension.validate();
	}

	@Test
	public void theNamedArgumentShorthandRejectsUnknownOptions() {
		try {
			extension.action(Map.of("fallback", ShellPluginExtension.ACTION_LAUNCH));
			fail("an unknown option should not be accepted");
		} catch (InvalidPluginConfigurationException e) {
			assertTrue("message is '"+e.getMessage()+"'", e.getMessage().contains("Unknown option 'fallback'"));
		}
	}

	@Test
	public void theNamedArgumentShorthandRejectsANonStringValue() {
		try {
			extension.action(Map.of("default", 42));
			fail("a default that is not a String should not be accepted");
		} catch (InvalidPluginConfigurationException e) {
			assertTrue("message is '"+e.getMessage()+"'", e.getMessage().contains("expected 'launch' or 'install'"));
		}
	}

	private void withLauncher() {
		extension.launcher = extension.new Launcher();
		extension.launcher.script("bin/reactor.sh");
	}

	private void assertValidateFails(String expectedFragment) {
		try {
			extension.validate();
			fail("validate() should have complained about '"+expectedFragment+"'");
		} catch (InvalidPluginConfigurationException e) {
			assertTrue("message is '"+e.getMessage()+"'", e.getMessage().contains(expectedFragment));
		}
	}
}
