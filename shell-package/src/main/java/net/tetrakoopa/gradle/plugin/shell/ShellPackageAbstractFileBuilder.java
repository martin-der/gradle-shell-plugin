package net.tetrakoopa.gradle.plugin.shell;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import lombok.Cleanup;
import net.tetrakoopa.gradle.SystemUtil;

public abstract class ShellPackageAbstractFileBuilder implements Closeable {
	
	protected final File outputFile;
	protected final OutputStream outputStream;

	private final Charset charset = StandardCharsets.UTF_8;

	protected ShellPackageAbstractFileBuilder(File targetFile) throws FileNotFoundException {
		this.outputFile = targetFile;
		this.outputStream = new FileOutputStream(outputFile);
	}

	@Override
	public void close() throws IOException {
		this.outputStream.close();
	}

	protected void writeClassPathResource(String path) throws IOException {
		@Cleanup
		final InputStream inputStreamDispencePre = ResourceUtil.getClassPathResource(path);
		inputStreamDispencePre.transferTo(outputStream);
	}

	protected void write(String text) throws IOException {
		outputStream.write(text.getBytes(charset));
	}

	protected void insertProperty(String propertyName, Enum<?> propertyValue) throws IOException {
		insertProperty(propertyName, propertyValue == null ? null : propertyValue.name());
	}

	protected void insertProperty(String propertyName, boolean propertyValue) throws IOException {
		insertProperty(propertyName, propertyValue ? 1 : 0);
	}

	protected void insertProperty(String propertyName, int propertyValue) throws IOException {
		insertProperty(propertyName, String.valueOf(propertyValue), "i");
	}

	protected void insertProperty(String propertyName, String propertyValue) throws IOException {
		insertProperty(propertyName, propertyValue, null);
	}

	protected void insertProperty(String propertyName, String propertyValue, String extraOption) throws IOException {
		write("declare -r"+(extraOption != null ? extraOption : "")+" "+propertyName+"=");
		if (propertyValue == null || propertyValue.isBlank())
			write("\n");
		else
			write(propertyValue.trim()+"\n");
	}

	protected void makeExecutable(boolean b, boolean c) throws IOException {
		SystemUtil.makeExecutable(outputFile, true, false);
	}

}
