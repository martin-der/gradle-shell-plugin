package net.tetrakoopa.gradle.plugin.shell.packaage.extension.documentation

import net.tetrakoopa.gradle.plugin.shell.packaage.resource.DefaultUseSpec
import net.tetrakoopa.gradle.plugin.shell.packaage.resource.UseSpec

class Documentation {

	boolean tableOfContent

	final List<Lot> lots = new ArrayList<>()

	def lot(Closure closure) {
		Lot lot = new DocumentationLot()
		lot.__configure(closure, "documentation lot")
		lots.add(lot)
	}
}
