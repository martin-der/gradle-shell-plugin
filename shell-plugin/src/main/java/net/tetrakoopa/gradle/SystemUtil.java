package net.tetrakoopa.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SystemUtil {
	
	public static void makeExecutable(File file, boolean toGroup, boolean toOther) throws IOException {
		final Path path = file.toPath();

		final Set<PosixFilePermission> permissions = Files.readAttributes(path,PosixFileAttributes.class).permissions();

		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		if (toGroup) {
			permissions.add(PosixFilePermission.GROUP_EXECUTE);
		}
		if (toOther) {
			permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		}
		
		Files.setPosixFilePermissions(path, permissions);
	}

}
