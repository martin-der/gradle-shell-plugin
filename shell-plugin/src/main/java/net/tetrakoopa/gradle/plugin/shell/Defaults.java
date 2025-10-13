package net.tetrakoopa.gradle.plugin.shell;


import lombok.experimental.UtilityClass;

@UtilityClass
public class Defaults {

	public static void setShellPackageDefaultsConfiguration(ShellPluginExtension extension) {
		// extension.ready = false;
		extension.distributionName = null;
		extension.version = null;
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
