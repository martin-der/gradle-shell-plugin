package net.tetrakoopa.gradle.plugin.shell.packaage.util

class MultilinePropertyWriter extends PrintWriter {

	MultilinePropertyWriter(File file, String encoding) throws FileNotFoundException, UnsupportedEncodingException {
		super(file, encoding)
	}

	MultilinePropertyWriter appendProperty(String name, String value) {
		append("${name}=")
		.append(value == null? "" : value.replaceAll('\n', '\n-\t'))
		.append('\n')
		return this
	}

}
