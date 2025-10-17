package net.tetrakoopa.gradle.plugin.usual;

import org.gradle.util.internal.ConfigureUtil;

import groovy.lang.Closure;

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
		this.relativeDir = relativeDir;
	}
	public String getInto() {
		return this.relativeDir;
	}
}
