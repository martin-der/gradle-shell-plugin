package net.tetrakoopa.gradle.plugin.shell.documentation;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class DocumentationToMarkdownGeneratoratorTest {
	
	@Test
	public void dummy() throws IOException {
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		DocumentationToMarkdownGeneratorator.convert(input("""
				# @description
				# Some function
				# @arg1 pouet 
				doStuff() {
					echo "done !"
				}	
				"""), outstream);

				assertEquals("""	

					* [doStuff](#doStuff)

					## doStuff## doStuff

					@description
					Some function""",
					new String(outstream.toByteArray(), StandardCharsets.UTF_8));
	}

	private static InputStream input(String content) {
		return new ByteArrayInputStream(content.getBytes());
	}
}
