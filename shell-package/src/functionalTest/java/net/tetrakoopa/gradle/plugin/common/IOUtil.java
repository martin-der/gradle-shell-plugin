package net.tetrakoopa.gradle.plugin.common;

import java.util.Comparator;
import java.util.stream.Stream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.InvalidParameterException;


public class IOUtil {

	public static void deleteDirectory(File file) throws IOException {
		deleteDirectory(file.toPath());
	}
	public static void deleteDirectory(File file, boolean mustExist) throws IOException {
		deleteDirectory(file.toPath(), mustExist);
	}
	public static void deleteDirectory(Path path) throws IOException {
		deleteDirectory(path, true);
	}

	public static void deleteDirectory(Path path, boolean mustExist) throws IOException {
		final boolean exists = path.toFile().exists();
		if(!exists) {
			if (mustExist) {
				throw new InvalidParameterException();
			}
			return;
		}
		System.out.println("Deleting '"+path.toFile().getAbsolutePath()+"'");
		try (Stream<Path> paths = Files.walk(path)) {
			paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}
	
	// public static void copyDirectory0(File source, File target) throws IOException {
	// 	Path sourceDir = source.toPath();
	// 	Path destinationDir = target.toPath();
	// 	try {

	// 		Files.walk(sourceDir).forEach(sourcePath -> {
	// 			final Path targetPath = destinationDir.resolve(sourceDir.relativize(sourcePath));
				
	// 			try {
	// 				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
	// 			} catch (IOException ioex) {
	// 				throw new IllegalStateException(ioex.getMessage(), ioex);
	// 			}
	// 		});
	// 	} catch (IllegalStateException isex) {
	// 		throw new IOException(isex.getMessage(), isex.getCause());
	// 	}
	// }
	public static void copyFile(File source, File target) throws IOException {
		copyFile(source.toPath(), target.toPath());
	}
	public static void copyFile(Path source, Path target)
            throws IOException {
           Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
	}

	public static void copyDirectory(File source, File target) throws IOException {
		copyDirectory(source.toPath(), target.toPath());
	}
	public static void copyDirectory(Path source, Path target)
            throws IOException {

        // is this a directory?
        if (Files.isDirectory(source)) {

            //if target directory exist?
            if (Files.notExists(target)) {
                // create it
                Files.createDirectories(target);
                System.out.println("Directory created : " + target);
            }

            // list all files or folders from the source, Java 1.8, returns a stream
            // doc said need try-with-resources, auto-close stream
            try (Stream<Path> paths = Files.list(source)) {

                // recursive loop
				try {
					paths.forEach(p -> {
						try {
							copyDirectory(p, target.resolve(source.relativize(p)));
						} catch (IOException ioex) {
							throw new IllegalStateException(ioex.getMessage(), ioex);
						}

					});
				} catch (IllegalStateException isex) {
					throw new IOException(isex.getMessage(), isex.getCause());
				}

            }

        } else {
            // if file exists, replace it
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

	public static void moveDirectory(File source, File target) throws IOException {
		moveDirectory(source.toPath(), target.toPath());

	}
	public static void moveDirectory(Path source, Path target) throws IOException {
		Files.move(source, target);
	}
}
