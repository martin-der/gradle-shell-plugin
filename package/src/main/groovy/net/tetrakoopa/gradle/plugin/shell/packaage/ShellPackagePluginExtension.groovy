package net.tetrakoopa.gradle.plugin.shell.packaage

import net.tetrakoopa.gradle.plugin.common.file.DefaultPathOrContentLocation
import net.tetrakoopa.gradle.plugin.common.file.PathOrContentLocation
import net.tetrakoopa.gradle.plugin.shell.packaage.exception.ShellPackagePluginException
import net.tetrakoopa.gradle.plugin.shell.packaage.extension.documentation.Documentation
import net.tetrakoopa.gradle.plugin.shell.packaage.resource.DefaultInstallSpec
import net.tetrakoopa.gradle.plugin.shell.packaage.resource.InstallSpec
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.util.ConfigureUtil

class ShellPackagePluginExtension {

	public final static String SHELL_PACKAGE_EXTENSION_NAME = "shell_package"

	private final Project project
	boolean ready

	ShellPackagePluginExtension(Project project) {
		this.project = project
	}
	class Output {
		private final Project project

		File distributionDirectory
		File documentationDirectory

		Output(Project project) { this.project = project}

		def distributionDirectory(String file) {
			this.distributionDirectory = file.startsWith('/') ? new File(file) : new File("${project.buildDir.absolutePath}/${file}")
		}
		def distributionDirectory(File file) {
			this.distributionDirectory = file
		}
		def documentationDirectory(String file) {
			this.documentationDirectory = file.startsWith('/') ? new File(file) : new File("${project.buildDir.absolutePath}/${file}")
		}
		def documentationDirectory(File file) {
			this.documentationDirectory = file
		}
	}
	class Banner {
		final PathOrContentLocation content = new DefaultPathOrContentLocation()
		Closure replace
	}

	class Information {
		class Maintainer {
			String name
			String email
		}
		Maintainer maintainer
	}

	class Installer {
		class Prefix {
			boolean useDefault
			private final List<String> alternatives = new ArrayList<>()
			def alternative(String prefix) {
				if (prefix == null) throw new IllegalArgumentException('Prefix cannot be null')
				//if (!prefix.startsWith('/') && !prefix.matches('^\\$')) throw new IllegalArgumentException("Invalid prefix '${prefix}' : Prefix be a absolute path or start with a variable")
				alternatives.add(prefix)
			}
			def alternatives(String... prefixes) {
				prefixes.each {prefix -> alternative(prefix)}
			}
		}
		class UserScript {
			final PathOrContentLocation script = new DefaultPathOrContentLocation()
			String question
			def script(Closure closure) { script.__configure(closure, "installer userScript script") }
		}
		class Licence {
			final PathOrContentLocation licence = new DefaultPathOrContentLocation()
			String preamble
			String agreementRequest
			def content(Closure closure) { licence.__configure(closure, "installer licence content")}
		}

		final Prefix prefix = new Prefix()
		final Licence licence = new Licence()
		final PathOrContentLocation readme = new DefaultPathOrContentLocation()
		final UserScript userScript = new UserScript()

		private final List<InstallSpec> installSpecs = new ArrayList<InstallSpec>()

		def component(Closure closure) {
			InstallSpec spec = ConfigureUtil.configure(closure, new DefaultInstallSpec())
			if (spec.importance == null) spec.importance = InstallSpec.Importance.MANDATORY
			if (!spec.name) throw new ShellPackagePluginException("Component must be named")
			if (installSpecs.any({s -> s.name == spec.name})) throw new ShellPackagePluginException("A component named '${spec.name}' already exists")
			installSpecs.add(spec)
		}

		boolean makeExecutable
		def makeExecutable(boolean executable) { this.executable = executable }

		def userScript(Closure closure) { ConfigureUtil.configure(closure, userScript) }
		def readme(Closure closure) { readme.__configure(closure, "installer readme")}
		def licence(Closure closure) { ConfigureUtil.configure(closure, licence) }
	}

	CopySpec source
	String distributionName
	String version
	Information information
	Banner banner = new Banner()
	final Output output = new Output(project)
	Documentation documentation = new Documentation()
	Installer installer = new Installer()

	def source(Closure closure) {
		source = project.copySpec(closure)
	}

	def information(Closure closure) { ConfigureUtil.configure(closure, information) }
	def output(Closure closure) { ConfigureUtil.configure(closure, output) }
	def documentation(Closure closure) { ConfigureUtil.configure(closure, documentation) }
	def banner(Closure closure) { ConfigureUtil.configure(closure, banner) }
	def installer(Closure closure) { ConfigureUtil.configure(closure, installer) }
}

