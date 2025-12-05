package net.tetrakoopa.gradle;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Function;

import org.gradle.api.Project;

import lombok.Cleanup;
import lombok.Getter;

public interface ModifiablePathOrContentLocation extends PathOrContentLocation {

	Function<String, String> getModify();

	InputStream getModified(Project project) throws IOException;

	@Getter
	public class Default extends PathOrContentLocation.Default implements ModifiablePathOrContentLocation {

		private Function<String, String> modify;

		public void modify(Function<String, String> closure) {
			modify = closure;
		}

		public InputStream getModified(Project project) throws IOException {
			final File file = super.resolve(project);
			final Function<String, String> modify = getModify();
			if (modify == null) {
				return new FileInputStream(file);
			}
			// final var cl = new MethodClosure(this, "shouldBeUsedAsClosure");
			@Cleanup
			final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			String line;
			while ((line = reader.readLine()) != null) {
				line = modify.apply(line);
				output.write(line.getBytes());
			}
			return new ByteArrayInputStream(output.toByteArray());
		}
	}

}
