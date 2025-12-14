package net.tetrakoopa.gradle.plugin.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
// import lombok.experimental.UtilityClass;

// @UtilityClass
public class SystemUtil {

	private SystemUtil() {}

	public static class ProcessExecutor {

		private final String[] command;
		private OutputStream output;
		private OutputStream error;

		public ProcessExecutor(String... command) {
			this.command = command;
		}

		public ProcessExecutor output(OutputStream output) {
			this.output = output;
			return this;
		}
		public ProcessExecutor error(OutputStream error) {
			this.error = error;
			return this;
		}


		public void run() throws IOException {
			run(null);
		}
		public void run(Consumer<OutputStream> stdinConsumer) throws IOException {
		 
			final ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();

			final OutputStream stdin = process.getOutputStream();
			final InputStream stdout = process.getInputStream();
			final InputStream stderr = process.getErrorStream();

			if (stdinConsumer != null) {
				stdinConsumer.accept(stdin);
			}

			consume(stdout, output);

			consume(stderr, error);
		}
	}

	private static void consume(InputStream input, OutputStream output) throws IOException {
		if (output != null) {
			input.transferTo(output);
		} else {
			final byte[] buffer = new byte[1000]; 
			int read;
			while ((read = input.read(buffer)) > 0) {

			}
		}

	}

}
