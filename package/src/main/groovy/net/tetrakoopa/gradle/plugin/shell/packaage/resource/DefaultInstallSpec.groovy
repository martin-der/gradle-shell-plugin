package net.tetrakoopa.gradle.plugin.shell.packaage.resource

class DefaultInstallSpec extends DefaultUseSpec implements InstallSpec {

	String name
	Importance importance
	String description

	@Override
	void name(String name) { setName(name) }
	@Override
	void setName(String name) {
		FORBIDDEN_CHARS.each { c -> if (name.indexOf((int)c)>=0) throw new IllegalArgumentException("Invalid name '${name}, name must not contain any of '${new String(FORBIDDEN_CHARS).replace('\n','\\n')}'")}
		this.name = name
	}

	@Override
	void importance(Importance importance) { setImportance(importance) }
	@Override
	void setImportance(Importance importance) {
		this.importance = importance
	}
	@Override
	void setImportance(String importanceName) {
		for (Importance possibleImportance : Importance.values()) {
			if (possibleImportance.name() == importanceName.toUpperCase()) {
				setImportance(possibleImportance)
				return
			}
		}
		throw new IllegalArgumentException("No such Importance '${importanceName}'")
	}

	@Override
	void description(String description) { setDescription(description) }
}
