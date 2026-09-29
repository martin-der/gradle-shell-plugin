package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.tetrakoopa.gradle.ShellEscaper;
import net.tetrakoopa.gradle.SystemUtil;

@Getter @Setter
@Accessors(fluent = true, chain = true)
public class ShellPackageDispenserArchiveBuilder extends ShellPackageAbstractFileBuilder {

	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
	private final File sourceDirectory;

	private boolean makeExecutable;

	private boolean usePersistentTempFolder;

	private String applicationName;
	private String applicationVersion;

	
	private static final String UNKNOWN_APPLICATION_NAME = "unknown";
	private static final String VARIABLE_PERSISTENT_TEMP_FOLDER = "MDU_SD_PERSISTENT_TEMP_FOLDER";

	/** Terminator of the heredocs used to inline file content in the generated archive. */
	private static final String HEREDOC_TERMINATOR = "MDU_SD_EOF";
	
	public ShellPackageDispenserArchiveBuilder(File sourceDirectory, File targetFile) throws FileNotFoundException {
		super(targetFile);
		this.sourceDirectory = sourceDirectory;
	}

	public void build() throws IOException {

		// The recorder file is a Windows-only fallback : it carries the modes the dispenser could
		// not store on the filesystem. On a POSIX build the filesystem is authoritative.
		final Map<String, Integer> recordedFileModes = SystemUtil.isWindows()
			? FileModeRegistry.read(FileModeRegistry.registryFileFor(sourceDirectory))
			: Collections.emptyMap();

		writeClassPathResource("/template/extract-pre.sh");

		write("\n\n");

		insertProperty("MDU_SD_INSTALL_APPLICATION_LABEL", applicationName);
		insertProperty("MDU_SD_INSTALL_APPLICATION_NAME", applicationName);
		insertProperty("MDU_SD_INSTALL_APPLICATION_VERSION", applicationVersion);
		if (usePersistentTempFolder) {
			insertProperty(VARIABLE_PERSISTENT_TEMP_FOLDER, "mdu-shell-dispenser__"
			+(applicationName==null?UNKNOWN_APPLICATION_NAME:applicationName)
			+(applicationVersion==null?"":("__"+applicationVersion))+"__"+UUID.randomUUID().toString());
		}

		write("\n\n");

		writeClassPathResource("/template/extract-before-extraction.sh");

		write("\n\n");

		writeClassPathResource("/inlined/base64.sh");

		write("\n\n");		

		
		final Path sourceDirectoryPath = sourceDirectory.toPath();
		for (Path absolutePath : toIterable(Files.walk(sourceDirectoryPath).filter(Files::isRegularFile).iterator())) {
			final Path path = sourceDirectoryPath.relativize(absolutePath);
			final String pathString = path.toString().replace(File.separatorChar, '/');
			write("#\n");
			write("# File "+pathString+"\n");
			write("#\n");

			write("\n");

			final String absoluteEscapedPath = "${MDU_SD_INSTALL_TEMP_DIR}/"+shellEscapedString(pathString);

			if (path.getNameCount()>1) {
				final String escapedParentPath = shellEscapedString(path.getParent().toString().replace(File.separatorChar, '/'));
				write("mkdir -p \"${MDU_SD_INSTALL_TEMP_DIR}/"+escapedParentPath+"\"\n");
			}

			if (canBeInlinedAsText(absolutePath)) {
				write("sed 's/^X //' << 'MDU_SD_EOF' > \""+absoluteEscapedPath+"\"\n");
				try (Stream<String> stream = Files.lines(absolutePath)) {
						for (String l : toIterable(stream.iterator())) {
							write("X ");
							write(l);
							write("\n");
						}
				}										
				write("\nMDU_SD_EOF\n");
			} else {
				write("${MDU_SD_DECODE_BASE64} << 'MDU_SD_EOF' > \""+absoluteEscapedPath+"\"\n");
				@Cleanup
				final InputStream input = Files.newInputStream(absolutePath);
				write(Base64.getEncoder().encodeToString(input.readAllBytes()));
				write("\nMDU_SD_EOF\n");
			}
			write("\n");
			final int fileMode = recordedFileModes.getOrDefault(pathString, SystemUtil.getPermissions(absolutePath));
			write(String.format("""
					chmod %03o "%s" || {
						log_warning "Failed to set permission %03o to file '%s'"
					}
					""", fileMode, absoluteEscapedPath, fileMode, absoluteEscapedPath));
		};


		writeClassPathResource("/template/extract-post.sh");

		if (makeExecutable) {
			makeExecutable(true, false);
		}
	}

	private String shellEscapedString(String string) {
		// Paths land inside a double-quoted shell word, so only these five characters are special.
		return ShellEscaper.escapeForDoubleQuotes(string);
	}

	/**
	 * Decides whether a file can be embedded in the archive as readable text.
	 *
	 * <p>Text is inlined verbatim between {@code << 'MDU_SD_EOF'} and {@code MDU_SD_EOF}, which is
	 * far nicer to debug than base64 but has two requirements: the file must not contain bytes that
	 * would make the generated script misparse, and it must not contain a line equal to the heredoc
	 * terminator. A file violating the second rule is silently truncated at that line and the rest of
	 * the payload leaks into the script body, so such files are base64-encoded instead.
	 */
	private boolean canBeInlinedAsText(Path file) throws IOException {
		if (!isText(file)) {
			return false;
		}
		try (Stream<String> lines = Files.lines(file)) {
			return lines.noneMatch(HEREDOC_TERMINATOR::equals);
		}
	}

	private static boolean isText(Path file) throws IOException {
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
