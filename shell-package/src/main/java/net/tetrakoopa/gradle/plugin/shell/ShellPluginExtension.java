package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.util.internal.ConfigureUtil;
import groovy.lang.Closure;
import lombok.AccessLevel;
import lombok.Getter;
import net.tetrakoopa.gradle.ModifiablePathOrContentLocation;
import net.tetrakoopa.gradle.PathOrContentLocation;
import net.tetrakoopa.gradle.plugin.exception.InvalidPluginConfigurationException;
import net.tetrakoopa.gradle.plugin.exception.InvalidPluginConfigurationException.ConfigurationPath;
import net.tetrakoopa.gradle.plugin.usual.InstallSpec;
import net.tetrakoopa.gradle.plugin.usual.ShellCallback;

/**
 * The {@code shell_package { }} DSL.
 *
 * <p>Every field is {@code final} and there is deliberately no class level {@code @Setter}: a
 * generated setter would let a build script bypass the checks that live in the {@code Closure}
 * methods, and those checks are the only thing standing between a typo and a silently broken
 * package. All validation that can run without executing tasks is funnelled through
 * {@link #validate()}, which is called once at the end of configuration and reports <em>every</em>
 * problem at once rather than one per build.
 */
@Getter
public class ShellPluginExtension implements InvalidPluginConfigurationException.ConfigurationPath.Builder {

	@Getter(AccessLevel.NONE)
	public static final String NAME = "shell_package";


	/**
	 * {@code name} is used verbatim as the generated archive's file name, so it is restricted to
	 * characters that are safe both in a file name and in a shell word.
	 */
	private static final String ARCHIVE_FILE_NAME_RULE = "[A-Za-z0-9._-]+\\z";

	/** Bash identifiers only: the launcher environment file is {@code source}d at runtime. */
	private static final Pattern SHELL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*\\z");

	/** The two first parameters the generated package understands. */
	public static final String ACTION_INSTALL = "install";
	public static final String ACTION_LAUNCH = "launch";

	@Getter
	public enum MultiActionModeStrategy {
		ACTION_MODE_PREFIX("action-mode-prefix"),
		DESAMBIGUATION_FUNCTION("disambiguation-function");

		private final String code;

		MultiActionModeStrategy(String code) {
			this.code = code;
		}

		public String getCode() { return code; }

		public static MultiActionModeStrategy byCode(String label) {
			if (label == null) {
				return null;
			}
			for (MultiActionModeStrategy possibleStrategy : MultiActionModeStrategy.values()) {
				if (possibleStrategy.code.equals(label)) {
					return possibleStrategy;
				}
			}
			throw new IllegalArgumentException("No such "+MultiActionModeStrategy.class.getName()+"."+label
				+", expected one of : "+Arrays.stream(MultiActionModeStrategy.values())
					.map(MultiActionModeStrategy::getCode).collect(Collectors.joining(", ")));
		}
	}

	@Getter
	public class MultiAction {
		private MultiActionModeStrategy mode;
		private ShellCallback desambiguation;

		/**
		 * Action the generated package performs when no first parameter names one, {@code null}
		 * when the {@code action} block does not ask for a default.
		 *
		 * <p>Validated by {@link ShellPluginExtension#validate()} : the shell template can only
		 * ever write {@value ShellPluginExtension#ACTION_INSTALL} or
		 * {@value ShellPluginExtension#ACTION_LAUNCH} into the package.
		 */
		private String defaultAction;

		/**
		 * Whether the build script wrote an {@code action { … }} block at all.
		 *
		 * <p>An {@code action} block without a {@code default} changes nothing about the generated
		 * package, so it is warned about — but only when the block was actually written; the
		 * extension always holds a {@link MultiAction} instance, even for a build script that
		 * never mentions {@code action}.
		 */
		private boolean configured;

		public void setMode(String label) {
			configured = true;
			mode = MultiActionModeStrategy.byCode(label);
		}
		public void setMode(MultiActionModeStrategy mode) {
			configured = true;
			this.mode = mode;
		}

		/** DSL form : {@code action { defaultAction 'launch' }}. */
		void defaultAction(String value) {
			declareDefaultAction(value);
		}

		/** Property form : {@code action { defaultAction = 'launch' }}. */
		void setDefaultAction(String value) {
			declareDefaultAction(value);
		}

		/**
		 * Also the entry point of the {@code action(default: 'launch')} shorthand, which is how
		 * the requested spelling survives Groovy's grammar : {@code default} is a reserved word,
		 * so {@code action { default = 'launch' }} and {@code action { default: 'launch' }} are
		 * parse errors, while a named-argument map happily uses it as a key.
		 */
		void declareDefaultAction(String value) {
			configured = true;
			this.defaultAction = value;
		}

		/**
		 * Present so that {@code action { desambiguation { … } }} reports "not implemented"
		 * instead of Groovy's "Could not find method desambiguation()".
		 */
		void desambiguation(Closure<ShellCallback> closure) {
			rejectUnsupported("action.desambiguation");
		}
	}

	@Getter
	public class TextFileSource {
		private File source;
		private Function<String, String> modify;

		public void source(Object file) {
			if (file == null) {
				throw new InvalidPluginConfigurationException(configurationPath("banner", "source"),
					"No path defined");
			}
			this.source = project.file(file);
		}

		public void modify(Closure<String> modify) {
			if (modify == null) {
				throw new InvalidPluginConfigurationException(configurationPath("banner", "modify"),
					"'modify' must not be null");
			}
			this.modify = line -> modify.call(line);
		}

	}

	@Inject
    public ShellPluginExtension(ObjectFactory objects, Project project) {
		this.project = project;
        this.name = objects.property(String.class);
		this.label = objects.property(String.class);
		this.distributionName = objects.property(String.class);
		this.version = objects.property(String.class);
        this.source = objects.property(CopySpec.class);
		this.source.convention(project.copySpec());
    }

	private final Project project;

    private final Property<String> name;
	private final Property<CopySpec> source;
    private final Property<String> label;
	private final Property<String> distributionName;
	private final Property<String> version;

	@Getter
	public class Information {
		public class Maintainer {
			String name;
			String email;
		}
		Maintainer maintainer;
		void maintainer(Closure<Maintainer> closure) { 
			rejectUnsupported("information.maintainer");
		}
	}

	@Getter
	public class Installer {
		public class Prefix {
			boolean useDefault;
			private final List<String> alternatives = new ArrayList<>();
			void alternative(String prefix) {
				if (prefix == null) throw new IllegalArgumentException("Prefix cannot be null");
				if (!prefix.startsWith("/") && !prefix.startsWith("$")) {
					throw new InvalidPluginConfigurationException(
						configurationPath("installer", "prefix", "alternative"),
						"Invalid prefix '"+prefix+"' : prefix must be an absolute path or start with a variable (e.g. '${HOME}/bin')");
				}
				alternatives.add(prefix);
			}
			void alternatives(String... prefixes) {
				for (String prefix: prefixes) alternative(prefix);
			}
		}
		public class UserScript {
			final PathOrContentLocation script = new PathOrContentLocation.Default();
			final Map<String, String> environment = new HashMap<>();
		}

		public class Licence {
			final PathOrContentLocation licence = new PathOrContentLocation.Default();
			String preamble;
			String agreementRequest;
		}

		final Prefix prefix = new Prefix();
		final Licence licence = new Licence();
		ModifiablePathOrContentLocation readme;
		final UserScript userScript = new UserScript();

		void prefix(Closure<Prefix> closure) {
			rejectUnsupported("installer.prefix");
		}

		void component(Closure<InstallSpec> closure) {
			rejectUnsupported("installer.component");
		}

		/**
		 * Whether the generated archive gets its executable bit set.
		 *
		 * <p>Defaults to {@code true} because that is what the dispenser has always done; opt out
		 * explicitly with {@code installer { makeExecutable false }}.
		 */
		boolean makeExecutable = true;
		void makeExecutable(boolean executable) { this.makeExecutable = executable; }

		void userScript(Closure<UserScript> closure) {
			rejectUnsupported("installer.userScript");
		}

		void readme(String location) {
			declareReadme(new ModifiablePathOrContentLocation.Default().forWhat("installer readme"), location);
		}
		void readme(Closure<PathOrContentLocation> closure) { 
			final ModifiablePathOrContentLocation.Default newReadme =
				new ModifiablePathOrContentLocation.Default().forWhat("installer readme");
			newReadme.__configure(closure, "installer readme");
			declareReadme(newReadme, null);
		}
		private void declareReadme(ModifiablePathOrContentLocation newReadme, String location) {
			// Silently keeping the last of two declarations is exactly the kind of surprise that is
			// impossible to debug from the generated package, so a second one is an error.
			if (readme != null) {
				throw new InvalidPluginConfigurationException(configurationPath("installer", "readme"),
					"Already defined. Remove the duplicate 'readme' declaration.");
			}
			if (location != null) {
				newReadme.setLocation(location);
			}
			readme = newReadme;
		}

		void licence(Closure<Licence> closure) {
			rejectUnsupported("installer.licence");
		}
	}

	@Getter
	public class Launcher {
		private String script;
		private String workingDirectory;
		private Map<String, String> environment = new HashMap<>();

		public void script(String script) {
			this.script = script;
		}
		public void environment(Map<String, String> environment) {
			this.environment = environment == null ? new HashMap<>() : new HashMap<>(environment);
		}
	}

	final MultiAction action = new MultiAction();

	final Information information = new Information();
	TextFileSource banner;
	final Installer installer = new Installer();
	Launcher launcher;

	/**
	 * DSL keys the user configured that this version cannot honour. Only used for a key that lives
	 * inside an otherwise supported block; a wholly unsupported block is rejected on the spot.
	 */
	private final Set<String> unsupportedOptions = new LinkedHashSet<>();

	/**
	 * Rejects a DSL block this version cannot honour, instead of accepting it and dropping it.
	 *
	 * <p>Accept-and-ignore is the worst possible behaviour for a build plugin: the build goes
	 * green, the artifact looks plausible, and nothing in the output says the setting was lost.
	 */
	private void rejectUnsupported(String optionPath) {
		throw new InvalidPluginConfigurationException(configurationPath(optionPath.split("\\.")),
			"This option is not implemented by this version of the plugin and would be silently ignored, "
			+ "so the generated package would not match your build script. Remove it, or file an issue if "
			+ "you need it.");
	}

	private void unsupported(String optionPath) {
		unsupportedOptions.add(optionPath);
	}

	void action(Closure<MultiAction> closure) {
		action.configured = true;
		ConfigureUtil.configure(closure, action);
	}

	/**
	 * Named-argument shorthand for the block form : {@code action(default: 'launch')}.
	 *
	 * <p>This is the only way to spell the option with the very name {@code default}, because
	 * Groovy reserves that word : inside a {@code shell_package { … }} closure both
	 * {@code action { default = 'launch' }} and {@code action { default: 'launch' }} fail to
	 * parse, while a map key is not an identifier and parses fine.
	 */
	void action(Map<String, Object> arguments) {
		if (arguments == null || arguments.isEmpty()) {
			action.configured = true;
			return;
		}
		for (Map.Entry<String, Object> option : arguments.entrySet()) {
			if (!"default".equals(option.getKey()) && !"defaultAction".equals(option.getKey())) {
				throw new InvalidPluginConfigurationException(configurationPath("action"),
					"Unknown option '"+option.getKey()+"', expected 'default' (as in "
					+ "action(default: '"+ACTION_LAUNCH+"')).");
			}
			final Object value = option.getValue();
			if (value != null && !(value instanceof String)) {
				throw new InvalidPluginConfigurationException(configurationPath("action", "default"),
					"'"+value+"' ("+value.getClass().getSimpleName()+") is not an action this package knows, "
					+ "expected '"+ACTION_LAUNCH+"' or '"+ACTION_INSTALL+"'.");
			}
			action.declareDefaultAction((String) value);
		}
	}

	void source(Action<CopySpec> action) {
		action.execute(source.get());
	}

	boolean keepTemporaryDirectory = true;
	void keepTemporaryDirectory(boolean keepIt) { this.keepTemporaryDirectory = keepIt; }
	/** Property-assignment form, so that {@code keepTemporaryDirectory = false} also works. */
	void setKeepTemporaryDirectory(boolean keepIt) { this.keepTemporaryDirectory = keepIt; }

	void information(Closure<Information> closure) { ConfigureUtil.configure(closure, information); }
	void banner(Closure<TextFileSource> closure) {
		if (banner != null) {
			throw new InvalidPluginConfigurationException(configurationPath("banner"),
				"Already defined as '"+banner.getSource()+"'. Remove the duplicate 'banner' declaration.");
		}
		banner = new TextFileSource();
		ConfigureUtil.configure(closure, banner);
		if (banner.getSource() == null) {
			throw new InvalidPluginConfigurationException(configurationPath("banner"), "No path defined");
		}
	}
	void installer(Closure<Installer> closure) { ConfigureUtil.configure(closure, installer); }
	void launcher(Closure<Launcher> closure) { 
		if (launcher != null) {
			throw new InvalidPluginConfigurationException(configurationPath("launcher"),
				"Already defined. Remove the duplicate 'launcher' declaration.");
		}
		launcher = new Launcher();
		ConfigureUtil.configure(closure, launcher); 
	}

	// ---------------------------------------------------------------------------------------------
	// Validation
	// ---------------------------------------------------------------------------------------------

	/**
	 * Reports <em>all</em> configuration problems at once.
	 *
	 * <p>Called at the end of project evaluation. A build script with three mistakes should take
	 * three rounds of "fix one, rebuild, find the next" otherwise, and each of those rounds is a
	 * full Gradle invocation.
	 */
	public void validate() {
		final List<String> problems = new ArrayList<>();

		validateName(problems);
		validateBanner(problems);
		validateReadme(problems);
		validateLauncher(problems);
		validateAction(problems);
		// Emptiness of 'source' is deliberately not checked here: which files a CopySpec selects is
		// only known once the copy has run, and resolving the tree at configuration time would both
		// cost real time on large projects and warn spuriously about sources generated by another
		// task. DispenserTask performs the one authoritative check, once the outcome is known.

		if (!unsupportedOptions.isEmpty()) {
			problems.add("These options are not implemented by this version of the plugin and would be "
				+ "silently ignored, so the package would not match your build script : "
				+ String.join(", ", unsupportedOptions)
				+ ". Remove them, or file an issue if you need them.");
		}

		if (!problems.isEmpty()) {
			throw new InvalidPluginConfigurationException(configurationPath(),
				"Invalid shell_package configuration :\n  - "+String.join("\n  - ", problems));
		}
	}

	private void validateName(List<String> problems) {
		if (name.isPresent() && !name.get().matches(ARCHIVE_FILE_NAME_RULE)) {
			problems.add("In shell_package > name : '"+name.get()+"' is used as the generated archive's file "
				+ "name, so only [A-Za-z0-9._-] is allowed (no spaces, no '/', no shell metacharacters). "
				+ "Use 'label' for a human readable name.");
		}
		if (distributionName.isPresent() && !distributionName.get().matches(ARCHIVE_FILE_NAME_RULE)) {
			problems.add("In shell_package > distributionName : '"+distributionName.get()+"' is used as the "
				+ "generated archive's file name, so only [A-Za-z0-9._-] is allowed.");
		}
	}

	private void validateBanner(List<String> problems) {
		if (banner == null) {
			return;
		}
		if (banner.getSource() == null) {
			problems.add("In shell_package > banner : no path defined, 'source' is required.");
		} else if (!banner.getSource().isFile()) {
			problems.add("In shell_package > banner > source : file '"+banner.getSource()+"' does not exist "
				+ "(relative paths are resolved against "+project.getProjectDir()+").");
		}
	}

	private void validateReadme(List<String> problems) {
		if (installer.readme == null) {
			return;
		}
		final File resolved;
		try {
			resolved = installer.readme.resolve(project);
		} catch (RuntimeException e) {
			// BothPathAndLocationDefined / EmptyPathAndLocation already say what is wrong.
			problems.add("In shell_package > installer > readme : "+e.getMessage());
			return;
		}
		if (!resolved.isFile()) {
			problems.add("In shell_package > installer > readme : file '"+resolved+"' does not exist "
				+ "(relative paths are resolved against "+project.getProjectDir()+").");
		}
	}

	private void validateLauncher(List<String> problems) {
		if (launcher == null) {
			return;
		}
		if (launcher.getScript() == null || launcher.getScript().isBlank()) {
			problems.add("In shell_package > launcher : a path to the script to execute is required with "
				+ "'launcher.script' (relative to the packaged content, e.g. 'bin/server.sh').");
			return;
		}
		final String script = launcher.getScript();
		if (script.indexOf('\\') >= 0) {
			problems.add("In shell_package > launcher > script : '"+script+"' contains a Windows path separator. "
				+ "Use '/' — the path is resolved against the packaged content directory inside a bash script.");
		}
		if (script.startsWith("/")) {
			problems.add("In shell_package > launcher > script : '"+script+"' must be relative to the packaged "
				+ "content root (e.g. 'bin/server.sh'), not an absolute path.");
		}
		if (launcher.getWorkingDirectory() != null) {
			unsupported("launcher.workingDirectory");
		}
		for (Map.Entry<String, String> entry : launcher.getEnvironment().entrySet()) {
			validateEnvironmentEntry(problems, entry.getKey(), entry.getValue());
		}
	}

	private void validateEnvironmentEntry(List<String> problems, String key, String value) {
		if (key == null || !SHELL_IDENTIFIER.matcher(key).matches()) {
			problems.add("In shell_package > launcher > environment : '"+key+"' is not a valid shell variable "
				+ "name (expected [A-Za-z_][A-Za-z0-9_]*). Names are written verbatim into a file that the "
				+ "generated installer 'source's.");
			return;
		}
		if (value == null) {
			return;
		}
		if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
			problems.add("In shell_package > launcher > environment > "+key+" : value must not contain a line "
				+ "break, it is written unescaped into a file that the generated installer 'source's.");
		}
	}

	private void validateAction(List<String> problems) {
		final MultiActionModeStrategy mode = action.getMode();
		if (mode == MultiActionModeStrategy.DESAMBIGUATION_FUNCTION) {
			problems.add("In shell_package > action : mode '"+MultiActionModeStrategy.DESAMBIGUATION_FUNCTION.getCode()
				+"' is not implemented by this version of the plugin, the generated package ignores it. "
				+ "Use mode '"+MultiActionModeStrategy.ACTION_MODE_PREFIX.getCode()+"'.");
		}
		if (!action.isConfigured()) {
			return;
		}

		final String defaultAction = action.getDefaultAction();
		if (defaultAction == null || defaultAction.isBlank()) {
			// Not an error : the package still builds, it just keeps requiring 'install' or
			// 'launch' as a first parameter, which is exactly what the block failed to change.
			project.getLogger().warn("In shell_package > action : the block is configured but no 'default' is set, "
				+ "so the generated package will still refuse to run without an explicit '"+ACTION_INSTALL+"' or '"
				+ ACTION_LAUNCH+"' first parameter. Add e.g. action { defaultAction '"+ACTION_LAUNCH+"' }.");
			return;
		}
		if (!ACTION_INSTALL.equals(defaultAction) && !ACTION_LAUNCH.equals(defaultAction)) {
			problems.add("In shell_package > action > default : '"+defaultAction+"' is not an action this package "
				+ "knows, expected '"+ACTION_LAUNCH+"' or '"+ACTION_INSTALL+"'.");
			return;
		}
		if (ACTION_LAUNCH.equals(defaultAction) && launcher == null) {
			problems.add("In shell_package > action > default : '"+ACTION_LAUNCH+"' cannot be the default of a "
				+ "package without a 'launcher' block — there would be nothing to launch.");
		}
	}
}
