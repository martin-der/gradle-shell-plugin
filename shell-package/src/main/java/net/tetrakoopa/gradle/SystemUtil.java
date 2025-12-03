package net.tetrakoopa.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.IOException;

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

	public static int getPermissions(Path path) throws IOException {
		final PosixFileAttributes attributes = Files.readAttributes(
			path, 
			PosixFileAttributes.class
		);
		
		final Set<PosixFilePermission> permissions = attributes.permissions();
		return getOctalPermissions(permissions);
	}
    
    private static int getOctalPermissions(Set<PosixFilePermission> permissions) {
        int octal = 0;
        
        for (PosixFilePermission perm : permissions) {
			octal |= switch (perm) {
				case OWNER_READ -> 0400;
				case OWNER_WRITE -> 0200;
				case OWNER_EXECUTE -> 0100;
				case GROUP_READ -> 0040;
				case GROUP_WRITE -> 0020;
				case GROUP_EXECUTE -> 0010;
				case OTHERS_READ -> 0004;
				case OTHERS_WRITE -> 0002;
				case OTHERS_EXECUTE -> 0001;
			};
    	}
	        
		return octal;
    }

}
