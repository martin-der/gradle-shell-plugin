package net.tetrakoopa.gradle;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.util.internal.ConfigureUtil;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;
import net.tetrakoopa.gradle.plugin.exception.BothPathAndLocationDefinedException;
import net.tetrakoopa.gradle.plugin.exception.EmptyPathAndLocationException;

public interface PathOrContentLocation {

	void setPath(File path);

	File resolve(Project project);

	File getPath();

	void path(File path);

	void setLocation(String location);

	/** A relative location */
	String getLocation();

	void location(String location);

	boolean isDefined();

	@Getter @Setter
	public class Default implements PathOrContentLocation {

		public void __configure(Closure<? extends PathOrContentLocation> closure, String forWhat) {
			ConfigureUtil.configure(closure, this);
			checkOnlyOneDefinition(forWhat);
		}

		private File path;

		private String location;

		@Override
		public File resolve(Project project) {
			if (project == null) {
				throw new NullPointerException("'project' must be provided");
			}
			if (path != null) {
				return path;
			}
			if (location != null) {
				return project.file(location);
			}
			throw new EmptyPathAndLocationException(null);
			
		}

		@Override
		public boolean isDefined() {
			return location != null || path != null;
		}

		private void checkOnlyOneDefinition(String forWhat) {
			if (location != null && path != null)
				throw new BothPathAndLocationDefinedException(forWhat);
		}

		@Override
		public void path(File path) {
			setPath(path);
			checkOnlyOneDefinition(null);
		}

		@Override
		public void location(String location) {
			setLocation(location);
			checkOnlyOneDefinition(null);
		}
	}

}
