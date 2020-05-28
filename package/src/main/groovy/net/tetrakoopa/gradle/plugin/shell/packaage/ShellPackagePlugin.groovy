package net.tetrakoopa.gradle.plugin.shell.packaage

import net.tetrakoopa.gradle.plugin.common.file.PathOrContentLocation
import net.tetrakoopa.gradle.plugin.shell.common.AbstractShellProjectPlugin
import net.tetrakoopa.gradle.plugin.shell.packaage.ShellPackagePluginExtension.Information
import net.tetrakoopa.gradle.plugin.shell.packaage.exception.ShellPackagePluginException
import net.tetrakoopa.gradle.plugin.shell.packaage.extension.documentation.Documentation
import net.tetrakoopa.gradle.plugin.shell.packaage.extension.documentation.Lot.ToDocumentationConverter
import net.tetrakoopa.gradle.plugin.shell.packaage.extension.documentation.implementation.ShellCommentToMarkdown
import net.tetrakoopa.gradle.plugin.shell.packaage.helper.CommonPackageHelper
import net.tetrakoopa.gradle.plugin.shell.packaage.helper.installer.InstallerHelper
import net.tetrakoopa.gradle.plugin.shell.packaage.resource.UseFileDetails
import net.tetrakoopa.gradle.plugin.shell.packaage.usual.BannerContentReplacers
import net.tetrakoopa.gradle.plugin.shell.packaage.usual.NameMapper
import net.tetrakoopa.gradle.plugin.shell.packaage.util.MultilinePropertyWriter
import net.tetrakoopa.gradle.plugin.shell.packaage.util.ResourceHelper
import net.tetrakoopa.poignee.bundledresources.BundledResourcesPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip

import java.util.jar.Attributes
import java.util.jar.Manifest

import static ShellPackagePluginExtension.SHELL_PACKAGE_EXTENSION_NAME

class ShellPackagePlugin extends AbstractShellProjectPlugin implements Plugin<Project> {

	public static final String ID = "net.tetrakoopa.shell-package"

	public static final String TASK_NAME_DOCUMENTATION = "documentation"
	public static final String TASK_NAME_PACKAGE_ZIP = "packageZip"
	public static final String TASK_NAME_PACKAGE_TGZ = "packageTgz"
	public static final String TASK_NAME_PACKAGES = "package"
	public static final String TASK_NAME_INSTALLER = "installer"
	public static final String TASK_NAME_EXECUTABLE_INSTALLER = "executableInstaller"
	public static final String TASK_PREPARE = "shell-package_common"

	interface BannerLineReplacer {
		String replace(Map<String, String> values, String line)
	}

	public static class Internal {
		Date buildDate
		File toolResourcesDir
		File workingDir
		File intermediateSourcesDirectory
		File installerFilesRootDir
		ConfigurableFileCollection intermediateSources
		final List<FileCopyDetails> sourceDetails = new ArrayList<>()
		final Map<String, String> descriptionValues = new HashMap<>()
	}

	private static class Context {
		final Internal internal
		final Project project

		Context(Internal internal, Project project) {
			this.internal = internal; this.project = project
		}
	}

	private static void insertProperty(File destination, String propertyName, String propertyValue) {
		if (propertyValue == null || propertyValue == "")
			destination.append("${propertyName}=\n")
		else
			destination.append("${propertyName}=\"${propertyValue}\"\n")
	}

	private static void insertFileDeclaredAsProperty(File destination, Context context, File file, String propertyName, String fileCopiedName, BannerLineReplacer replacer) {
		if (file != null) {
			if (replacer) {
				context.project.copy {
					from file
					into context.internal.installerFilesRootDir
					rename { name -> "${fileCopiedName}" }
					filter { line -> replacer.replace(context.internal.descriptionValues, line) }
				}
			} else
				context.project.copy {
					from file
					into context.internal.installerFilesRootDir
					rename { name -> "${fileCopiedName}" }
				}
			insertProperty(destination, propertyName, "./${fileCopiedName}")
		} else {
			insertProperty(destination, propertyName, null)
		}
	}

	private static void insertFileOrContentAndDeclareAsProperty(File destination, Context context, PathOrContentLocation pathOrLocation, String propertyName, String fileCopiedName) {
		insertFileOrContentAndDeclareAsProperty(destination, context, pathOrLocation, propertyName, fileCopiedName, null)
	}

	private static void insertFileOrContentAndDeclareAsProperty(File destination, Context context, PathOrContentLocation pathOrLocation, String propertyName, String fileCopiedName, BannerLineReplacer replacer) {
		if (pathOrLocation.defined()) {
			if (pathOrLocation.path != null) {
				insertFileDeclaredAsProperty(destination, context, pathOrLocation.path, propertyName, fileCopiedName, replacer)
			} else {
				insertProperty(destination, propertyName, "./content/${pathOrLocation.location}")
			}
		} else {
			destination.append("${propertyName}=\n")
		}
	}

	private static void setShellPackageDefaultsConfiguration(Project project) {
		ShellPackagePluginExtension shell_package = (ShellPackagePluginExtension) project.getExtensions().findByName(SHELL_PACKAGE_EXTENSION_NAME)
		shell_package.ready = false
		shell_package.distributionName = null
		shell_package.version = null
		shell_package.installer.prefix.useDefault = true
		shell_package.installer.prefix.alternatives('/usr/bin', '${HOME}/bin', '/usr/local/bin')
		shell_package.installer.licence.licence.location = shell_package.installer.licence.licence.path = null
		shell_package.installer.readme.location = shell_package.installer.readme.path = null
		shell_package.installer.userScript.script.location = shell_package.installer.userScript.script.path = null
		shell_package.installer.userScript.question = null
		shell_package.output.distributionDirectory = null
		shell_package.output.distributionDirectory = null
	}

	private static void makeShellPackageConfiguration(Project project) {
		ShellPackagePluginExtension shell_package = (ShellPackagePluginExtension) project.getExtensions().findByName(SHELL_PACKAGE_EXTENSION_NAME)
		if (shell_package.ready) return
		shell_package.ready = true
		if (shell_package.source == null) throw new ShellPackagePluginException("No source file(s) defined")
		if (shell_package.distributionName == null) shell_package.distributionName = "${project.name}-${project.version}"
		if (shell_package.version == null) shell_package.version = project.version
		if (shell_package.output.distributionDirectory == null) shell_package.output.distributionDirectory = project.buildDir
		if (shell_package.output.distributionDirectory == null) shell_package.output.distributionDirectory = new File(project.buildDir.absolutePath + '/doc')
	}

	void apply(Project project) {

		addProjectExtensions(project)

		project.ext.SCRIPT_NAME_MAPPER = new NameMapper()

		project.extensions.create(SHELL_PACKAGE_EXTENSION_NAME, ShellPackagePluginExtension, project)
		setShellPackageDefaultsConfiguration(project)

		project.afterEvaluate {
			makeShellPackageConfiguration(project)

			final Internal internal = new Internal()
			internal.buildDate = new Date()
			internal.toolResourcesDir = BundledResourcesPlugin.unpackBundledResources(project, ID, "tool")
			internal.workingDir = project.file("${project.buildDir}/n4k-shell")
			internal.intermediateSourcesDirectory = new File(internal.workingDir.absolutePath + '/intermediateSources')

			internal.descriptionValues.put('version', project.shell_package.version)
			final Information information = project.shell_package.information
			internal.descriptionValues.put('maintainer.name', information?.maintainer?.name != null ?: "unknown_maintainer")
			internal.descriptionValues.put('maintainer.email', information?.maintainer?.name != null ?: "not@an.email.address")
			internal.descriptionValues.put('build.date', internal.buildDate.format( 'dd-MMM-yy hh:mm' ))

			project.shell_package.source.eachFile {
				FileCopyDetails details -> internal.sourceDetails.add(details)
			}

			project.shell_package.documentation.lots.forEach { lot -> lot.converter = new ShellCommentToMarkdown() }

			final Context context = new Context(internal, project)

			addTasks(context)
		}
	}


	void addTasks(Context context) {

		ShellPackagePluginExtension shell_package = (ShellPackagePluginExtension) context.project.getExtensions().findByName(SHELL_PACKAGE_EXTENSION_NAME)

		final Project project = context.project
		final Internal internal = context.internal

		def prepareSourcesTask = project.task(TASK_PREPARE) {

			ext.inputFiles = shell_package.source

			doLast {
				project.copy {
					with shell_package.source
					into internal.intermediateSourcesDirectory
				}
				internal.intermediateSources = project.files(internal.intermediateSourcesDirectory)
			}
		}

		def documentationTask = project.task(TASK_NAME_DOCUMENTATION, dependsOn: prepareSourcesTask) {

			ext.inputFiles = shell_package.source

			doLast {

				if (!existsInPath("gawk")) throw new IllegalStateException("Task '${TASK_NAME_DOCUMENTATION}' requires command 'gawk'")

				final Documentation documentation = shell_package.documentation

				documentation.lots.forEach { lot ->
					File outputDir = lot.outputDir ?: shell_package.output.distributionDirectory

					if (!outputDir.exists()) outputDir.mkdirs()

					final ToDocumentationConverter converter = lot.converter

					converter.construct(project)

					try {
						internal.sourceDetails.forEach { FileCopyDetails fileCopyDetails ->

							final UseFileDetails details = CommonPackageHelper.useFileDetails(fileCopyDetails, lot.into)

							final RelativePath relativeSourcePath = details.relativeSourcePath

							File file = relativeSourcePath.getFile(internal.intermediateSourcesDirectory)

							if (true /* documentation.lot.eachFile != null */) {

								if (!details.originalName.endsWith(".sh"))
									details.exclude()
							}

							if (!details.excluded) {
								File document = details.relativePath.getFile(outputDir)
								File script = file
								document = new File(document.absolutePath + '.md')
								document.parentFile.mkdirs()

								try {
									converter.convert(project, details.originalName, script, document)
								} catch (Exception ex) {
									throw new ShellPackagePluginException("Document '${script.absolutePath}' creation failed : "+ex.getMessage(), ex)
								}

								// Remove the documentation if it's empty
								if (!lot.keepBlankFile)
									if (!(document.length() > 0) || document.text.matches("^[\n \t]*\$")) document.delete()
							}
						}
					} finally {
						converter.destroy(project)
					}

				}

			}
		}

		def packageZipTask = project.task(TASK_NAME_PACKAGE_ZIP, type: Zip, dependsOn: [prepareSourcesTask, documentationTask]) {

			archiveBaseName = shell_package.distributionName
			from internal.intermediateSourcesDirectory

			destinationDirectory = shell_package.output.distributionDirectory
		}

		def packageTGZTask = project.task(TASK_NAME_PACKAGE_TGZ, type: Tar, dependsOn: [prepareSourcesTask, documentationTask]) {

			archiveBaseName = shell_package.distributionName
			from internal.intermediateSourcesDirectory

			destinationDirectory = project.file(shell_package.output.distributionDirectory)

			compression = Compression.GZIP
			archiveExtension = 'tar.gz'
		}

		final String installerWorkingDir = "${internal.workingDir}/installer"
		final File sharFile = project.file("${installerWorkingDir}/${shell_package.distributionName}.shar")

		def installerTask = project.task(TASK_NAME_INSTALLER, dependsOn: prepareSourcesTask) {

			doLast {

				internal.installerFilesRootDir = context.project.file("${installerWorkingDir}/files")

				if (!existsInPath("shar")) throw new IllegalStateException("Task '${TASK_NAME_INSTALLER}' requires command 'shar'")

				project.copy {
					from internal.intermediateSources
					into "${internal.installerFilesRootDir.absolutePath}/content"
				}
				File installShFile = project.file("${internal.installerFilesRootDir.absolutePath}/install.sh")
				if (installShFile.exists()) installShFile.delete()
				installShFile.append(new File("${internal.toolResourcesDir}/install/template/install/install-pre.sh").text)

				insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.installer.readme, "readme_to_show", "README")

				insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.installer.licence.licence, "licence_to_show", "LICENCE")

				insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.banner.content, "banner_to_display", "banner.txt", BannerContentReplacers.MUSTACHE_SPACER_PARAMETER_REPLACER)

				insertFileOrContentAndDeclareAsProperty(installShFile, context, shell_package.installer.userScript.script, "user_script_to_execute", "user_post_install")
				insertProperty(installShFile, "user_script_question", shell_package.installer.userScript.question)

				installShFile.append(new File("${internal.toolResourcesDir}/install/template/install/install-post.sh").text)

				if (shell_package.installer.installSpecs.empty) {
					ResourceHelper.addCopyAll(shell_package.installer.installSpecs)
				}

				InstallerHelper.addComponents(internal.sourceDetails, shell_package.installer.installSpecs, new File(internal.installerFilesRootDir,'component'))

				InstallerHelper.addScripts(internal, project)

				MultilinePropertyWriter config = new MultilinePropertyWriter(new File(internal.installerFilesRootDir, 'config'), 'utf-8')

				config.appendProperty('ui.type','humble')
				config.appendProperty('ui.theme','desert')
				config.appendProperty('prefix.alternatives', shell_package.installer.prefix.alternatives.collect {it}.join('\n'))
				config.close()

				project.exec {
					workingDir internal.installerFilesRootDir
					// --submitter=who@where
					// --archive-name=${shell_package.distributionName}
					commandLine 'shar', '--quiet', '--quiet-unshar', '.'
					standardOutput = new FileOutputStream(sharFile)
				}

				def autoInstallerFile = project.file("${shell_package.output.distributionDirectory}/${shell_package.distributionName}.run")
				if (autoInstallerFile.exists()) autoInstallerFile.delete()

				autoInstallerFile.append(new File("${internal.toolResourcesDir}/install/template/extract-pre.sh").text)


				autoInstallerFile.append("\n")
				autoInstallerFile.append("# *** Technical variables *** \n")
				insertProperty(autoInstallerFile, "MDU_SHAR_VERSION", getSharVersion(project))
				insertProperty(autoInstallerFile, "MDU_INSTALLER_VERSION", getVersionFromJar())
				autoInstallerFile.append("\n")
				autoInstallerFile.append("# *** Application variables *** \n")
				insertProperty(autoInstallerFile, "MDU_INSTALL_APPLICATION_NAME", "${project.name}")
				insertProperty(autoInstallerFile, "MDU_INSTALL_APPLICATION_LABEL", "${project.name}")
				insertProperty(autoInstallerFile, "MDU_INSTALL_APPLICATION_VERSION", "${shell_package.version}")
				autoInstallerFile.append("\n")

				autoInstallerFile.append(cleanShar(sharFile.text))
				autoInstallerFile.append(new File("${internal.toolResourcesDir}/install/template/extract-post.sh").text)

			}

		}

		def executableInstallerTask = project.task(TASK_NAME_EXECUTABLE_INSTALLER, dependsOn: installerTask) {
			project.exec {
				commandLine 'chmod', '+x', sharFile.absolutePath
			}
		}


		def packageTask = project.task(TASK_NAME_PACKAGES, dependsOn: [packageTGZTask, packageZipTask, installerTask])

		project.task('shell-build', dependsOn: [documentationTask, packageTask, installerTask]) {}
	}

	private String getSharVersion(project) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream()
		project.exec {
			commandLine 'shar', '--version'
			standardOutput = stream
		}
		String version = stream.toString()
		int pos = version.indexOf('\n')
		if (pos>=0) version = version.substring(0, pos)
		return version.replaceAll('"','\\\\"')
	}

	private String cleanShar(String shar) {
		shar = shar - ~/^(#[^\n]*\n)*/
		shar = shar.replaceAll('\nexit[ ]+0[\n ]*$', "\n")
		return  shar
	}

	private String getVersionFromJar() {
		final Attributes attributes = getJarManifestAttributes(ShellPackagePlugin.class)
		return attributes.getValue('Build-Revision')
	}
	private Attributes getJarManifestAttributes(Class clazz) {
		final String className = clazz.getSimpleName() + ".class"
		final String classPath = clazz.getResource(className).toString()
		if (!classPath.startsWith("jar")) {
			throw new IllegalArgumentException("Class ${clazz.getName()} is not inside a jar")
		}
		final String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF"
		final Manifest manifest = new Manifest(new URL(manifestPath).openStream())
		return manifest.getMainAttributes()
	}
}

