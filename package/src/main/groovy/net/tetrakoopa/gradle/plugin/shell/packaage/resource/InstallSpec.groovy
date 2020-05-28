package net.tetrakoopa.gradle.plugin.shell.packaage.resource

interface InstallSpec extends UseSpec {

	public final char[] FORBIDDEN_CHARS = '\n/$\\!:%'.toCharArray()

	enum Importance {
		MANDATORY, RECOMMENDED, OPTIONAL, DISCOURAGED
	}

	void name(String name)
	void setName(String name)
	String getName()

	void description(String description)
	void setDescription(String description)
	String getDescription()

	void importance(Importance importance)
	void setImportance(Importance importance)
	void setImportance(String importance)
	Importance getImportance()
}