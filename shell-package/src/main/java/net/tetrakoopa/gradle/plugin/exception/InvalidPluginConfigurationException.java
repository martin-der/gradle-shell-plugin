package net.tetrakoopa.gradle.plugin.exception;

import lombok.Getter;

@Getter
public class InvalidPluginConfigurationException extends RuntimeException {

	private final ConfigurationPath path;
	
	public InvalidPluginConfigurationException(ConfigurationPath path, String message) { super(buildMessage(path, message)); this.path = path; }
	public InvalidPluginConfigurationException(ConfigurationPath path, String message, Throwable cause) { super(buildMessage(path, message), cause); this.path = path; }

	private static String buildMessage(ConfigurationPath path, String message) {
		return new StringBuilder()
			.append("In ")
			.append(String.join(">", path.parts))
			.append(" : ")
			.append(message)
			.toString();
	}

	@Getter
	public static class ConfigurationPath {

		public interface Builder {
			default ConfigurationPath configurationPath(String... path) {
				return new ConfigurationPath(path);
			}
		}

		private final String[] parts;

		public ConfigurationPath(String[] parts) {
			this.parts = parts;
		}
	}
}
