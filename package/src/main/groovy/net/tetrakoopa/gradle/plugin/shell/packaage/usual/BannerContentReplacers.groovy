package net.tetrakoopa.gradle.plugin.shell.packaage.usual

import net.tetrakoopa.gradle.plugin.shell.packaage.ShellPackagePlugin

import java.util.regex.Pattern

class BannerContentReplacers {

	public static final Pattern MUSTACHE_CATCHER_PARAMETER_MATCHER = Pattern.compile('(\\{\\{:? *([a-zA-Z_]+[a-zA-Z_01-9]*( *\\. *[a-zA-Z_]+[a-zA-Z_01-9]*)*) *:?\\}\\})')

	public static final Pattern MUSTACHE_SPACER_PARAMETER_MATCHER = Pattern.compile('(\\{\\{~? *([a-zA-Z_]+[a-zA-Z_01-9]*( *\\. *[a-zA-Z_]+[a-zA-Z_01-9]*)*) *~?\\}\\})')

	enum ReplacementType {
		ALIGN_LEFT, ALIGN_RIGHT, ALIGN_CENTER, SMALLEST
	}

	private abstract static class AbstractFixedWidthReplacer implements ShellPackagePlugin.BannerLineReplacer {

		protected abstract ReplacementType getReplacementType(String mustache)

		protected abstract Pattern getRegex()

		@Override
		String replace(Map<String, String> values, String line) {
			return line.replaceAll(getRegex(), {
				String all, String mustache, String property, rab ->

					final ReplacementType replacementType = getReplacementType(mustache)

					final int size = mustache.length()

					final String key = property.replaceAll(' ','')

					if (!values.containsKey(key)) {
						int keyLength = key.length()
						if (keyLength+2<size) {
							final String value = "[${key}]"
							if (replacementType == ReplacementType.ALIGN_RIGHT)
								return value.padLeft(size)
							if (replacementType == ReplacementType.ALIGN_LEFT)
								return value.padRight(size)
							if (replacementType == ReplacementType.ALIGN_CENTER)
								return value.center(size)
							return value
						}
						else if (keyLength+2>size)
							return "[${key.substring(size)}]"
						return "[${key}]"
					}

					String value = values.get(key)
					int valueLength = value.length()
					if (valueLength<size) {
						if (replacementType == ReplacementType.ALIGN_RIGHT)
							return value.padLeft(size)
						if (replacementType == ReplacementType.ALIGN_LEFT)
							return value.padRight(size)
						if (replacementType == ReplacementType.ALIGN_CENTER)
							return value.center(size)
						return value
					} else if (valueLength>size)
						value = value.substring(size)
					return value
			})
		}

	}

	public static ShellPackagePlugin.BannerLineReplacer MUSTACHE_SPACER_PARAMETER_REPLACER = new AbstractFixedWidthReplacer() {

		protected Pattern getRegex() { return BannerContentReplacers.MUSTACHE_SPACER_PARAMETER_MATCHER }

		protected ReplacementType getReplacementType(String mustache) {
			final boolean leftSpacer = mustache.startsWith("{{~")
			final boolean rightSpacer = mustache.endsWith("~}}")

			if (leftSpacer && rightSpacer)
				return ReplacementType.ALIGN_CENTER
			if (rightSpacer)
				return ReplacementType.ALIGN_LEFT
			if (leftSpacer)
				return ReplacementType.ALIGN_RIGHT
			return ReplacementType.SMALLEST
		}
	}
	public static ShellPackagePlugin.BannerLineReplacer MUSTACHE_CATCHER_PARAMETER_REPLACER = new AbstractFixedWidthReplacer() {

		protected Pattern getRegex() { return BannerContentReplacers.MUSTACHE_CATCHER_PARAMETER_MATCHER }

		protected ReplacementType getReplacementType(String mustache) {
			final boolean leftCatcher = mustache.startsWith("{{:")
			final boolean rightCatcher = mustache.endsWith(":}}")

			if (leftCatcher && rightCatcher)
				return ReplacementType.SMALLEST
			if (leftCatcher)
				return ReplacementType.ALIGN_LEFT
			if (rightCatcher)
				return ReplacementType.ALIGN_RIGHT
			return ReplacementType.ALIGN_CENTER
		}

	}
}
