package net.tetrakoopa.gradle;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.util.internal.ConfigureUtil;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;
import net.tetrakoopa.gradle.plugin.exception.BothPathAndLocationDefinedException;
import net.tetrakoopa.gradle.plugin.exception.EmptyPathAndLocationException;
import net.tetrakoopa.gradle.plugin.exception.ShellPackagePluginException;

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

		/**
		 * Human readable name of what is being configured, used to make error messages point at the
		 * right DSL key. Set by {@link #__configure(Closure, String)}.
		 */
		private String forWhat;

		public void __configure(Closure<? extends PathOrContentLocation> closure, String forWhat) {
			// Set before configuring so that errors raised from within the closure can name it.
			this.forWhat = forWhat;
			ConfigureUtil.configure(closure, this);
			checkOnlyOneDefinition();
		}

		/** Binds this instance to a DSL key, for instances built outside of a closure. */
		public Default forWhat(String forWhat) {
			this.forWhat = forWhat;
			return this;
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
			throw new EmptyPathAndLocationException(forWhat);
		}

		@Override
		public boolean isDefined() {
			return location != null || path != null;
		}

		private void checkOnlyOneDefinition() {
			if (location != null && path != null)
				throw new BothPathAndLocationDefinedException(forWhat);
		}

		@Override
		public void path(File path) {
			// Checked before assigning, otherwise the first value is lost and the error cannot say
			// which of the two was set first.
			if (location != null) throw new BothPathAndLocationDefinedException(forWhat);
			setPath(path);
		}

		@Override
		public void location(String location) {
			if (path != null) throw new BothPathAndLocationDefinedException(forWhat);
			setLocation(location);
		}

		/**
		 * Guards against an explicitly empty value, which would otherwise sail past
		 * {@link #isDefined()} style checks and only blow up much later, deep inside a task.
		 */
		@Override
		public void setLocation(String location) {
			if (location != null && location.isBlank()) {
				throw new ShellPackagePluginException(
					"'location' must not be blank" + (forWhat != null ? " for " + forWhat : ""));
			}
			this.location = location;
		}
	}

}
