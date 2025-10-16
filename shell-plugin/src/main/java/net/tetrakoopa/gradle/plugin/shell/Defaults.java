package net.tetrakoopa.gradle.plugin.shell;


import lombok.experimental.UtilityClass;
import net.tetrakoopa.gradle.plugin.shell.ShellPluginExtension.MultiActionModeStrategy;

@UtilityClass
public class Defaults {

	public static void setShellPackageDefaultsConfiguration(ShellPluginExtension extension) {
		// extension.ready = false;
		extension.distributionName = null;
		extension.version = null;
		extension.action.setMode(MultiActionModeStrategy.ACTION_MODE_PREFIX.getCode());
		extension.installer.prefix.useDefault = true;
		extension.installer.prefix.alternatives("/usr/bin", "${HOME}/bin", "/usr/local/bin");
		// extension.installer.licence.licence.location = shell_package.installer.licence.licence.path = null;
		// extension.installer.readme.location = shell_package.installer.readme.path = null;
		// shell_package.installer.userScript.script.location = shell_package.installer.userScript.script.path = null;
		// shell_package.installer.userScript.question = null;
		// shell_package.output.distributionDirectory = null;
		// shell_package.output.distributionDirectory = null;
	}
	
}
