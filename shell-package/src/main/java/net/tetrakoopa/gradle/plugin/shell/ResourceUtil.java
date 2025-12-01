package net.tetrakoopa.gradle.plugin.shell;

import java.io.FileNotFoundException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceUtil {

	public static InputStream getClassPathResource(String name) throws FileNotFoundException {
		return getClassPathResource(name, ResourceUtil.class);
	}

	public static InputStream getClassPathResource(String name, Class<?> classLoader) throws FileNotFoundException {
			
		if (! name.startsWith("/")) {
			throw new IllegalArgumentException("Resource path '"+name+"' must start with '/'");
		}

		final String resourcePath = "/dispenser"+name;
		final InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
		if (inputStream == null) {
			throw new FileNotFoundException("Unknown resource '"+resourcePath+"'");
		}

		return inputStream;
    }

}
