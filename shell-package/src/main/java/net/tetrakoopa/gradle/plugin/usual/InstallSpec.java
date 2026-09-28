package net.tetrakoopa.gradle.plugin.usual;

public interface InstallSpec extends UseSpec {

	/**
	 * Component names end up as shell identifiers and as directory names in the generated package,
	 * so they are restricted to characters that are safe in both. Note the trailing {@code \z}
	 * rather than {@code $}: {@code $} also matches just before a final line terminator, which would
	 * let a name smuggle a newline past this check.
	 */
	public static final String NAMING_RULE = "[A-Za-z0-9_-]+\\z";

	enum Importance {
		MANDATORY, RECOMMENDED, OPTIONAL, DISCOURAGED
	}

	void name(String name);
	void setName(String name);
	String getName();

	void description(String description);
	void setDescription(String description);
	String getDescription();

	void importance(Importance importance);
	void setImportance(Importance importance);

	/**
	 * Case-insensitive {@link #importance(Importance)} for the Groovy DSL.
	 *
	 * <p>Deliberately <em>not</em> a {@code setImportance(String)} overload: Groovy resolves
	 * {@code importance = 'OPTIONAL'} to the enum setter and reports its own, unhelpful coercion
	 * error, making a string overload dead code that can only ever be reached from Java.
	 */
	void importance(String importance);

	Importance getImportance();
}
