package net.tetrakoopa.gradle.plugin.usual;

import org.gradle.util.internal.ConfigureUtil;

import groovy.lang.Closure;
import net.tetrakoopa.gradle.PathOrContentLocation;
import net.tetrakoopa.gradle.plugin.exception.InvalidPluginConfigurationException;
import net.tetrakoopa.gradle.plugin.exception.InvalidPluginConfigurationException.ConfigurationPath;

public class ShellCallback {

	private final PathOrContentLocation script = new PathOrContentLocation.Default();

	private String method;

	public boolean isDefined() {
		return !script.isDefined() && method != null && !method.isEmpty();
	}

	public void __configure(Closure<ShellCallback> closure, ConfigurationPath path) { 
		ConfigureUtil.configure(closure, this);
		if (!isDefined()) {
			throw new InvalidPluginConfigurationException(path, "Both 'script' and 'method' must be defined");
		}
	}

	
}
