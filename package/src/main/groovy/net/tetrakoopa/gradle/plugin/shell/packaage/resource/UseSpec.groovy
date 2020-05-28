package net.tetrakoopa.gradle.plugin.shell.packaage.resource

interface UseSpec {

	interface ProcessingSpec {
		void process(UseFileDetails useFileDetails)
	}

	void eachFile(ProcessingSpec processingSpec)

	void into(String relativeDir)
	String getInto()

}
