package net.tetrakoopa.gradle.plugin.usual;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DefaultInstallSpec extends DefaultUseSpec implements InstallSpec {

	private String name;
	private Importance importance;
	private String description;

	@Override
	public void name(String name) { setName(name); }

	@Override
	public void importance(Importance importance) { setImportance(importance); }

	@Override
	public void setImportance(String importanceName) {
		for (Importance possibleImportance : Importance.values()) {
			if (possibleImportance.name() == importanceName.toUpperCase()) {
				setImportance(possibleImportance);
				return;
			}
		}
		throw new IllegalArgumentException("No such Importance '"+importanceName+"'");
	}

	@Override
	public void description(String description) { setDescription(description); }

	@Override
	public void setImportance(Importance importance) {
		this.importance = importance;
	}
}
