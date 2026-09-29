package net.tetrakoopa.gradle.plugin.shell;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists, in the build directory, the Unix modes of the files the plugin creates so that they can
 * be applied to the produced archive.
 *
 * <p>On Windows the filesystem has no notion of Unix permission bits, so
 * {@link net.tetrakoopa.gradle.SystemUtil#getPermissions SystemUtil.getPermissions} has no other
 * choice than to report the default 0644 for every file. As a result the archive loses the
 * executable bit of the scripts the plugin generates itself (the dispenser and the launcher
 * reactor). To bridge that gap on Windows only, the dispenser task records the mode of those files
 * into a simple text file in the build directory, and the archive builder lets the recorded mode
 * win over the filesystem one. On a POSIX build the mode is simply read back from the filesystem.</p>
 */
public class FileModeRegistry {

	public static final String FILE_NAME = "file-modes.txt";

	private FileModeRegistry() {
	}

	/** The registry sits next to the exploded package directory so that the archive builder never walks it. */
	public static File registryFileFor(File explodedDirectory) {
		return new File(explodedDirectory.getParentFile(), FILE_NAME);
	}

	public static void write(File registryFile, Map<String, Integer> modes) throws IOException {
		final File parent = registryFile.getAbsoluteFile().getParentFile();
		if (!parent.isDirectory()) {
			Files.createDirectories(parent.toPath());
		}
		final StringBuilder content = new StringBuilder();
		for (Map.Entry<String, Integer> entry : modes.entrySet()) {
			content.append(String.format("%03o ./%s\n", entry.getValue(), entry.getKey()));
		}
		Files.write(registryFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
	}

	/** @return the recorded modes, keyed by the path relative to the exploded package directory, in forward-slash form. */
	public static Map<String, Integer> read(File registryFile) throws IOException {
		if (!registryFile.isFile()) {
			return Collections.emptyMap();
		}
		final Map<String, Integer> modes = new LinkedHashMap<String, Integer>();
		for (String line : Files.readAllLines(registryFile.toPath(), StandardCharsets.UTF_8)) {
			final String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
				continue;
			}
			final int separator = trimmed.indexOf(' ');
			if (separator <= 0) {
				continue;
			}
			final String rawMode = trimmed.substring(0, separator);
			String path = trimmed.substring(separator + 1).trim();
			while (path.startsWith("./")) {
				path = path.substring(2);
			}
			if (path.isEmpty()) {
				continue;
			}
			try {
				modes.put(path, Integer.parseInt(rawMode, 8));
			} catch (NumberFormatException e) {
				// ignore malformed line
			}
		}
		return modes;
	}

}