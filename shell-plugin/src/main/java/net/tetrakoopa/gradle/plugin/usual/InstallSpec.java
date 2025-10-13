package net.tetrakoopa.gradle.plugin.usual;

public interface InstallSpec extends UseSpec {

	public static final String NAMING_RULE = "^[a-zA-Z01-9_-]+$";

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
	void setImportance(String importance);
	Importance getImportance();
}
