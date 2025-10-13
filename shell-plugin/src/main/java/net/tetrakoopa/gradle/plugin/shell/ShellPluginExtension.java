package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.util.internal.ConfigureUtil;

import groovy.lang.Closure;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.tetrakoopa.gradle.PathOrContentLocation;
import net.tetrakoopa.gradle.plugin.exception.ShellPackagePluginException;
import net.tetrakoopa.gradle.plugin.usual.DefaultInstallSpec;
import net.tetrakoopa.gradle.plugin.usual.InstallSpec;

@Getter @Setter
public class ShellPluginExtension {

	@Getter(AccessLevel.NONE)
	public static final String NAME = "shell_package";

	@Getter
	public enum DualActionModeStrategy {
		DEFAULT_TO_LAUNCHER("default-to-laucher"),
		REQUIRE_ACTION_MODE("require-action-mode");

		private final String label;

		DualActionModeStrategy(String label) {
			this.label = label;
		}

		public static DualActionModeStrategy byLabel(String label) {
			if (label == null) {
				return null;
			}
			for (DualActionModeStrategy possibleStrategy : DualActionModeStrategy.values()) {
				if (possibleStrategy.label.equals(label)) {
					return possibleStrategy;
				}
			}
			throw new IllegalArgumentException("No such "+DualActionModeStrategy.class.getName()+"."+label);
		}
	}

	@Inject
    public ShellPluginExtension(ObjectFactory objects, Project project) {
		this.project = project;
        this.name = objects.property(String.class);
        this.source = objects.property(CopySpec.class);
        // this.message = objects.property(String);
    }

	private final Project project;

    private Property<String> name;

	public class Output {

		File distributionDirectory;
		File documentationDirectory;

		final File buildDirectory;

		public Output() { 
			this.buildDirectory = project.getLayout().getBuildDirectory().get().getAsFile();
		}

		void distributionDirectory(String file) {
			this.distributionDirectory = file.startsWith("/") ? new File(file) : new File(buildDirectory, file);
		}
		void distributionDirectory(File file) {
			this.distributionDirectory = file;
		}
		void documentationDirectory(String file) {
			this.documentationDirectory = file.startsWith("/") ? new File(file) : new File(buildDirectory, file);
		}
		void documentationDirectory(File file) {
			this.documentationDirectory = file;
		}
	}
	public class Banner {
		final PathOrContentLocation content = new PathOrContentLocation.Default();
		Closure<String> replace;
	}

	@Getter
	public class Information {
		public class Maintainer {
			String name;
			String email;
		}
		Maintainer maintainer;
		void maintainer(Closure<Maintainer> closure) { 
			maintainer = new Maintainer();
			ConfigureUtil.configure(closure, maintainer); 
		}
	}

	public class Installer {
		public class Prefix {
			boolean useDefault;
			private final List<String> alternatives = new ArrayList<>();
			void alternative(String prefix) {
				if (prefix == null) throw new IllegalArgumentException("Prefix cannot be null");
				//if (!prefix.startsWith('/') && !prefix.matches('^\\$')) throw new IllegalArgumentException("Invalid prefix '${prefix}' : Prefix be a absolute path or start with a variable")
				alternatives.add(prefix);
			}
			void alternatives(String... prefixes) {
				Stream.of(prefixes).forEach(prefix -> alternative(prefix));
			}
		}
		public class UserScript {
			final PathOrContentLocation script = new PathOrContentLocation.Default();
			String question;
			// def script(Closure closure) { script.__configure(closure, "installer userScript script") }
		}
		public class Licence {
			final PathOrContentLocation licence = new PathOrContentLocation.Default();
			String preamble;
			String agreementRequest;
			// void content(Closure closure) { licence.__configure(closure, "installer licence content")}
		}

		final Prefix prefix = new Prefix();
		final Licence licence = new Licence();
		final PathOrContentLocation readme = new PathOrContentLocation.Default();
		final UserScript userScript = new UserScript();

		private final List<InstallSpec> installSpecs = new ArrayList<InstallSpec>();

		void component(Closure<InstallSpec> closure) {
			final InstallSpec spec = ConfigureUtil.configure(closure, new DefaultInstallSpec());
			if (spec.getImportance() == null) spec.setImportance(InstallSpec.Importance.MANDATORY);
			if (spec.getName() == null) throw new ShellPackagePluginException("Component must be named");
			if (!spec.getName().matches(InstallSpec.NAMING_RULE)) throw new ShellPackagePluginException("Component name must match '"+InstallSpec.NAMING_RULE+"'");
			if (installSpecs.stream().anyMatch(s -> s.getName().equals(spec.getName()))) throw new ShellPackagePluginException("A component named '"+spec.getName()+"' already exists");
			installSpecs.add(spec);
		}

		boolean makeExecutable;
		// void makeExecutable(boolean executable) { this.executable = executable }

		void userScript(Closure<UserScript> closure) { ConfigureUtil.configure(closure, userScript); }
		void readme(Closure<PathOrContentLocation> closure) { ((PathOrContentLocation.Default)readme).__configure(closure, "installer readme"); }
		void licence(Closure<Licence> closure) { ConfigureUtil.configure(closure, licence); }
	}
	@Getter @Setter
	public class Launcher {
		private String script;
		private String workingDirectory;
		private Map<String, String> environment = new HashMap<>();
	}

	private DualActionModeStrategy actionMode;
	public void setActionMode(String label) {
		actionMode = DualActionModeStrategy.byLabel(label);
	}

	Property<CopySpec> source;
	String distributionName;
	String version;
	final Information information = new Information();
	final Banner banner = new Banner();
	// final Output output = new Output(project);
	// Documentation documentation = new Documentation();
	final Installer installer = new Installer();
	Launcher launcher;

	void source(Closure<CopySpec> closure) {
		source.set(project.copySpec(closure));
	}

	void information(Closure<Information> closure) { ConfigureUtil.configure(closure, information); }
	// void output(Closure closure) { ConfigureUtil.configure(closure, output); }
	// void documentation(Closure closure) { ConfigureUtil.configure(closure, documentation); }
	void banner(Closure<Banner> closure) { ConfigureUtil.configure(closure, banner); }
	void installer(Closure<Installer> closure) { ConfigureUtil.configure(closure, installer); }
	void launcher(Closure<Launcher> closure) { 
		launcher = new Launcher();
		ConfigureUtil.configure(closure, launcher); 
	}
}
