package net.tetrakoopa.gradle.plugin.shell.packaage.extension.documentation

import net.tetrakoopa.gradle.plugin.shell.packaage.resource.DefaultUseSpec
import net.tetrakoopa.gradle.plugin.shell.packaage.resource.UseSpec
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

class DocumentationLot extends DefaultUseSpec implements Lot, UseSpec {

	boolean tableOfContent = false

	boolean keepBlankFile = false

	File outputDir

	private ToDocumentationConverter converter

	def __configure(Closure closure, String forWhat) {
		ConfigureUtil.configure(closure, this)
	}

	@Override
	void setConverter(ToDocumentationConverter converter) {
		this.converter = converter
	}

	@Override
	void setConverter(Closure closure) {
		setConverter(new ToDocumentationConverter() {

			@Override
			void construct(Project project) {}

			@Override
			void convert(Project project, String originalName, File source, File destination) {
				closure(project, originalName, source, destination)
			}

			@Override
			void destroy(Project project) {}
		})
	}
}
