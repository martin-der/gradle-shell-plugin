package net.tetrakoopa.gradle.plugin.shell.packaage.util

import net.tetrakoopa.gradle.plugin.shell.packaage.resource.DefaultInstallSpec
import net.tetrakoopa.gradle.plugin.shell.packaage.resource.InstallSpec

class ResourceHelper {

	static void addCopyAll(List<InstallSpec> specs) {
		final InstallSpec all = new DefaultInstallSpec()
		all.name = 'all'
		all.description = 'Copy all resources'
		all.importance = InstallSpec.Importance.MANDATORY
		specs.add(all)
	}
}
