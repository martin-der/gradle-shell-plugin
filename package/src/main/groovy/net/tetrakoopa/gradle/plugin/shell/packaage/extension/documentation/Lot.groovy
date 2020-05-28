package net.tetrakoopa.gradle.plugin.shell.packaage.extension.documentation

import org.gradle.api.Project

interface Lot {

	interface ToDocumentationConverter {
		void construct(Project project)
		void convert(Project project, String originalName, File source, File destination)
		void destroy(Project project)
	}

	void setTableOfContent(boolean tableOfContent)
	boolean getTableOfContent()

	void setOutputDir(File outputDir)
	File getOutputDir()

	void setConverter(ToDocumentationConverter converter)
	void setConverter(Closure closure)

	void setKeepBlankFile(boolean keepBlankFile)
	boolean getKeepBlankFile()


}