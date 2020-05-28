package net.tetrakoopa.gradle.plugin.shell.packaage.resource

import org.gradle.util.ConfigureUtil

class DefaultUseSpec implements UseSpec {

	def __configure(Closure closure, String forWhat) {
		ConfigureUtil.configure(closure, this)
	}

	ProcessingSpec processingSpec
	String relativeDir

	@Override
	void eachFile(ProcessingSpec processingSpec) {
		this.processingSpec = processingSpec
	}

	@Override
	void into(String relativeDir) {
		this.relativeDir = relativeDir
	}
	String getInto() {
		return this.relativeDir
	}
}
