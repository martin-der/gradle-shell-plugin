package net.tetrakoopa.gradle.plugin.usual;

import org.gradle.util.internal.ConfigureUtil;

import groovy.lang.Closure;
import net.tetrakoopa.gradle.plugin.exception.ShellPackagePluginException;

public class DefaultUseSpec implements UseSpec {

	public void __configure(Closure<UseSpec> closure, String forWhat) {
		ConfigureUtil.configure(closure, this);
	}

	ProcessingSpec processingSpec;
	String relativeDir;

	@Override
	public void eachFile(ProcessingSpec processingSpec) {
		this.processingSpec = processingSpec;
	}

	@Override
	public void into(String relativeDir) {
		if (relativeDir != null) {
			// Guards the generated package against escaping the content root: an absolute path or a
			// '..' segment here would let a component install files outside of the package.
			if (relativeDir.startsWith("/") || relativeDir.startsWith("\\"))
				throw new ShellPackagePluginException(
					"'into' must be a relative path, got absolute path '" + relativeDir + "'");
			for (String segment : relativeDir.split("[/\\\\]")) {
				if (segment.equals("..")) {
					throw new ShellPackagePluginException(
						"'into' must not contain a '..' segment, got '" + relativeDir + "'");
				}
			}
		}
		this.relativeDir = relativeDir;
	}
	public String getInto() {
		return this.relativeDir;
	}
}
