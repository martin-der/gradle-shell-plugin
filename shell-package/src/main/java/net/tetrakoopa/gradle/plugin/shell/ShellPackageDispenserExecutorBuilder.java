package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter @Setter
@Accessors(fluent = true, chain = true)
public class ShellPackageDispenserExecutorBuilder extends ShellPackageAbstractFileBuilder {


	private String label;
	private String packageName = null;
	private String packageVersion = null;
	private ShellPluginExtension.MultiActionModeStrategy actionModeStrategy = null;
	private boolean showReadme;
	private boolean showBanner;
	private boolean executeUserScript;
	private String launcherScript;
	private boolean launcherScriptHasEnvironmentProperties;

	public ShellPackageDispenserExecutorBuilder(File targetFile) throws FileNotFoundException {
		super(targetFile);
	}
	

	public void build() throws IOException {

		writeClassPathResource("/template/dispense/dispense-pre.sh");
		

		// installShFile.append(new File("${internal.toolResourcesDir}/install/template/install/install-pre.sh").text);

		// insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.installer.readme, "readme_to_show", "README");

		// insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.installer.licence.licence, "licence_to_show", "LICENCE");

		// insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.banner.content, "banner_to_display", "banner.txt", BannerContentReplacers.MUSTACHE_SPACER_PARAMETER_REPLACER);

		// insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.installer.userScript.script, "user_script_to_execute", "user_post_install");
		write("\n");
		write("# -------------\n");
		write("# Configuration\n");
		write("# -------------\n");
		write("#\n");

		// insertProperty("mdu_sp_package_name", extension.distributionName);
		// insertProperty("mdu_sp_package_label", label);
		// insertProperty("mdu_sp_package_version", extension.version);
		// insertProperty("mdu_sp_action_mode_strategy", extension.getAction().getMode().name());
		// insertProperty("mdu_sp_show_readme", extension.installer.readme != null ? true : true );
		// insertProperty("mdu_sp_show_banner", extension.banner != null);
		// insertProperty("mdu_sp_execute_user_script", extension.installer.userScript.script != null);
		// insertProperty("mdu_sp_executable_reactor_script", extension.launcher == null ? "" : extension.launcher.getScript());

		insertProperty("mdu_sp_package_name", packageName);
		insertProperty("mdu_sp_package_label", label);
		insertProperty("mdu_sp_package_version", packageVersion);
		insertProperty("mdu_sp_action_mode_strategy", actionModeStrategy.name());
		insertProperty("mdu_sp_show_readme", showReadme );
		insertProperty("mdu_sp_show_banner", showBanner);
		insertProperty("mdu_sp_execute_user_script", executeUserScript);
		insertProperty("mdu_sp_executable_reactor_script", launcherScript);
		insertProperty("mdu_sp_executable_has_environment_properties", launcherScriptHasEnvironmentProperties);

		write("\n");
		write("\n");

		writeClassPathResource("/template/dispense/dispense-post.sh");

		makeExecutable(true, false);
	}


	// private static void insertFileDeclaredAsProperty(File destination, Context context, File file, String propertyName, String fileCopiedName, BannerLineReplacer replacer) {
	// 	if (file != null) {
	// 		if (replacer) {
	// 			context.project.copy {
	// 				from file
	// 				into context.internal.explodedPackageDir
	// 				rename { name -> "${fileCopiedName}" }
	// 				filter { line -> replacer.replace(context.internal.descriptionValues, line) }
	// 			}
	// 		} else
	// 			context.project.copy {
	// 				from file
	// 				into context.internal.installerFilesRootDir
	// 				rename { name -> "${fileCopiedName}" }
	// 			}
	// 		insertProperty(destination, propertyName, "./${fileCopiedName}")
	// 	} else {
	// 		insertProperty(destination, propertyName, null)
	// 	}
	// }

	// private static void insertFileOrContentAndDeclareAsProperty(File destination, Context context, PathOrContentLocation pathOrLocation, String propertyName, String fileCopiedName) {
	// 	insertFileOrContentAndDeclareAsProperty(destination, context, pathOrLocation, propertyName, fileCopiedName, null)
	// }

	// private static void insertFileOrContentAndDeclareAsProperty(File destination, Context context, PathOrContentLocation pathOrLocation, String propertyName, String fileCopiedName, BannerLineReplacer replacer) {
	// 	if (pathOrLocation.defined()) {
	// 		if (pathOrLocation.path != null) {
	// 			insertFileDeclaredAsProperty(destination, context, pathOrLocation.path, propertyName, fileCopiedName, replacer)
	// 		} else {
	// 			insertProperty(destination, propertyName, "./content/${pathOrLocation.location}")
	// 		}
	// 	} else {
	// 		destination.append("${propertyName}=\n")
	// 	}
	// }


}
