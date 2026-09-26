package net.tetrakoopa.gradle.plugin.usual;

import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import net.tetrakoopa.gradle.plugin.exception.ShellPackagePluginException;

@Getter @Setter
public class DefaultInstallSpec extends DefaultUseSpec implements InstallSpec {

	private String name;
	private Importance importance;
	private String description;

	@Override
	public void name(String name) {
		if (name != null && name.isBlank()) {
			throw new ShellPackagePluginException("Component name must not be blank");
		}
		setName(name);
	}

	@Override
	public void importance(Importance importance) { setImportance(importance); }

	@Override
	public void importance(String importanceName) {
		if (importanceName == null) {
			throw new ShellPackagePluginException("Component importance must not be null");
		}
		for (Importance possibleImportance : Importance.values()) {
			if (possibleImportance.name().equalsIgnoreCase(importanceName)) {
				setImportance(possibleImportance);
				return;
			}
		}
		throw new ShellPackagePluginException(
			"No such importance '" + importanceName + "', expected one of : "
			+ java.util.Arrays.stream(Importance.values()).map(Enum::name).collect(Collectors.joining(", ")));
	}

	@Override
	public void description(String description) { setDescription(description); }

	@Override
	public void setImportance(Importance importance) {
		this.importance = importance;
	}
}
