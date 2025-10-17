package net.tetrakoopa.gradle.plugin.shell;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.gradle.api.Project;

import lombok.Cleanup;
import net.tetrakoopa.gradle.SystemUtil;

public class ShellPackageDispenserBuilder implements Closeable {

	private final Internal internal;
	private final ShellPluginExtension extension;

	private final File dispenseFile;
	private final OutputStream outputStream;

	public ShellPackageDispenserBuilder(Project project, Internal internal, ShellPluginExtension extension) throws FileNotFoundException {
		this.internal = internal;
		this.extension = extension;
		this.dispenseFile = project.file(internal.explodedPackageDir+"/dispense.sh");
		this.outputStream = new FileOutputStream(dispenseFile);
	}
	

	public void build() throws FileNotFoundException, IOException {

		writeClassPathResource("/template/dispense/dispense-pre.sh");
		

		// installShFile.append(new File("${internal.toolResourcesDir}/install/template/install/install-pre.sh").text);

		// insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.installer.readme, "readme_to_show", "README");

		// insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.installer.licence.licence, "licence_to_show", "LICENCE");

		// insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.banner.content, "banner_to_display", "banner.txt", BannerContentReplacers.MUSTACHE_SPACER_PARAMETER_REPLACER);

		// insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.installer.userScript.script, "user_script_to_execute", "user_post_install");
		write("\n");
		write("#\n");
		write("# Configuration\n");
		write("#\n");

		insertProperty("mdu_sp_package_name", extension.distributionName);
		insertProperty("mdu_sp_package_label", internal.name);
		insertProperty("mdu_sp_package_version", extension.version);
		insertProperty("mdu_sp_action_mode_strategy", extension.getAction().getMode().name());
		insertProperty("mdu_sp_show_readme", extension.installer.readme.isDefined());
		insertProperty("mdu_sp_show_banner", extension.banner.isDefined());
		insertProperty("mdu_sp_execute_user_script", extension.installer.userScript.script.isDefined());
		insertProperty("mdu_sp_executable_reactor_script", extension.launcher.getScript());

		write("\n");
		write("\n");

		writeClassPathResource("/template/dispense/dispense-post.sh");

		SystemUtil.makeExecutable(dispenseFile, true, false);
	}

	private void write(String text) throws IOException {
		outputStream.write(text.getBytes());

	}

	private void writeClassPathResource(String path) throws IOException {
		@Cleanup
		final InputStream inputStreamDispencePre = ResourceUtil.getClassPathResource(path);
		inputStreamDispencePre.transferTo(outputStream);
	}

	private void insertProperty(String propertyName, Enum<?> propertyValue) throws IOException {
		insertProperty(propertyName, propertyValue == null ? null : propertyValue.name());
	}

	private void insertProperty(String propertyName, boolean propertyValue) throws IOException {
		insertProperty(propertyName, propertyValue ? 1 : 0);
	}

	private void insertProperty(String propertyName, int propertyValue) throws IOException {
		insertProperty(propertyName, String.valueOf(propertyValue), "i");
	}

	private void insertProperty(String propertyName, String propertyValue) throws IOException {
		insertProperty(propertyName, propertyValue, null);
	}

	private void insertProperty(String propertyName, String propertyValue, String extraOption) throws IOException {
		write("declare -r"+(extraOption != null ? extraOption : "")+" "+propertyName+"=");
		if (propertyValue == null || propertyValue.isBlank())
			write("\n");
		else
			write(propertyValue.trim()+"\n");
	}


	@Override
	public void close() throws IOException {
		this.outputStream.close();
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
