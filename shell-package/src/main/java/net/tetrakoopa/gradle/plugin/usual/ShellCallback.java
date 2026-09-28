package net.tetrakoopa.gradle.plugin.usual;

import org.gradle.util.internal.ConfigureUtil;

import groovy.lang.Closure;
import net.tetrakoopa.gradle.PathOrContentLocation;
import net.tetrakoopa.gradle.plugin.exception.InvalidPluginConfigurationException;
import net.tetrakoopa.gradle.plugin.exception.InvalidPluginConfigurationException.ConfigurationPath;

public class ShellCallback {

	private final PathOrContentLocation script = new PathOrContentLocation.Default();

	private String method;

	/**
	 * A callback is usable as soon as <em>one</em> of its two entry points is provided: either a
	 * script to run, or a method to dispatch to.
	 */
	public boolean isDefined() {
		return script.isDefined() || (method != null && !method.isEmpty());
	}

	public void __configure(Closure<ShellCallback> closure, ConfigurationPath path) { 
		final String[] parts = path.getParts();
		((PathOrContentLocation.Default) script).forWhat(parts[parts.length-1]);
		ConfigureUtil.configure(closure, this);
		if (!isDefined()) {
			throw new InvalidPluginConfigurationException(path,
				"At least one of 'script' or 'method' must be defined, neither is");
		}
	}

	
}
