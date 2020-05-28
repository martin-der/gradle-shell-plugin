package net.tetrakoopa.gradle.plugin.shell.packaage.usual

class NameMapper {

	static Closure REMOVE_SUFFIX = {
		s -> ((String)s).replaceFirst('^(.*)\\..+$', '$1')
	}

	static Closure TO_LOWER_CASE = {
		s -> s.toLowerCase()
	}

}
