package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Iterator;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.tetrakoopa.gradle.SystemUtil;

@Getter @Setter
@Accessors(fluent = true, chain = true)
public class ShellPackageDispenserArchiveBuilder extends ShellPackageAbstractFileBuilder {

	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
	private final File sourceDirectory;

	private boolean makeExecutable;
	
	public ShellPackageDispenserArchiveBuilder(File sourceDirectory, File targetFile) throws FileNotFoundException {
		super(targetFile);
		this.sourceDirectory = sourceDirectory;
	}

	public void build() throws IOException {

		writeClassPathResource("/template/extract-pre.sh");

		
		final Path sourceDirectoryPath = sourceDirectory.toPath();
		for (Path absolutePath : toIterable(Files.walk(sourceDirectoryPath).filter(Files::isRegularFile).iterator())) {
			final Path path = sourceDirectoryPath.relativize(absolutePath);
			write("#\n");
			write("# File "+path.toString()+"\n");
			write("#\n");

			if (path.getNameCount()>1) {
				write("mkdir -p '"+path.getParent()+"'\n");
			}

			final String escapedPath = shellEscapedString(path.toString());
			if (isText(absolutePath)) {
				write("sed 's/^X //' << 'MDU_SD_EOF' > '"+escapedPath+"' \n");
				try (Stream<String> stream = Files.lines(absolutePath)) {
						for (String l : toIterable(stream.iterator())) {
							write("X ");
							write(l);
							write("\n");
						}
				}										
				write("\nMDU_SD_EOF\n");
			} else {
				write("${MDU_SD_DECODE_BASE64} << 'MDU_SD_EOF' > '"+escapedPath+"' \n");
				write(Base64.getEncoder().encodeToString(Files.newInputStream(absolutePath).readAllBytes()));
				write("\nMDU_SD_EOF\n");
			}
			write("\n");
			final int fileMode = SystemUtil.getPermissions(absolutePath);
			write(String.format("""
					chmod %03o "%s" || {
						log_warning "Failed to set permission %03o to file '%s'"
					}
					""", fileMode, path, fileMode, path));
		};


		writeClassPathResource("/template/extract-post.sh");

		if (makeExecutable) {
			makeExecutable(true, false);
		}
	}

	private String shellEscapedString(String string) {
		// TODO escape single qquote et carriage return
		return string;
	} 

	private boolean isText(Path file) throws IOException {
		@Cleanup
		final InputStream input = Files.newInputStream(file);
		final byte[] buffer = new byte[1000];
		int l;
		while ((l = input.read(buffer))>0) {
			for (int i = 0; i<l ; i++) {
				final byte b  = buffer[i];
				if (b<32 && b!='\n' && b!='\r' && b!='\t') {
					return false;
				}
			}
		} 

		return true;
	}

	private static <T> Iterable<T> toIterable(Iterator<T> iterator) {
        return () -> iterator;
    }


}
