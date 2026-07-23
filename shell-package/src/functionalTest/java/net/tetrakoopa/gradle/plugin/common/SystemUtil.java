package net.tetrakoopa.gradle.plugin.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SystemUtil {

	public static class ProcessExecutor<E extends ProcessExecutor<E>> {

		private final String[] command;

		@Getter @Accessors(fluent = true)
		private OutputStream output;
		private Charset grabbedOutputCharset;

		@Getter @Accessors(fluent = true)
		private OutputStream error;
		private Charset grabbedErrorCharset;

		public ProcessExecutor(String... command) {
			this.command = command;
		}

		public E output(OutputStream output) {
			this.output = output;
			return (E)this;
		}
		public E error(OutputStream error) {
			this.error = error;
			return (E)this;
		}

		public E grabOutput(Charset charset) {
			this.output = new ByteArrayOutputStream();
			this.grabbedOutputCharset = charset;
			return (E)this;
		}
		public E grabOutput() {
			return grabOutput(StandardCharsets.UTF_8);
		}
		public String grabbedOutput() {
			if (grabbedOutputCharset == null) {
				throw new IllegalStateException("Output not grabbed");
			}
			return new String(((ByteArrayOutputStream)output).toByteArray(), this.grabbedOutputCharset);
		}
		public E grabError(Charset charset) {
			this.error = new ByteArrayOutputStream();
			this.grabbedErrorCharset = charset;
			return (E)this;
		}
		public E grabError() {
			return grabError(StandardCharsets.UTF_8);
		}
		public String grabbedError() {
			if (grabbedErrorCharset == null) {
				throw new IllegalStateException("Error not grabbed");
			}
			return new String(((ByteArrayOutputStream)error).toByteArray(), this.grabbedErrorCharset);
		}



		public int run() throws IOException, InterruptedException {
			return run(null);
		}
		public int run(Consumer<OutputStream> stdinConsumer) throws IOException, InterruptedException {
		 
			final ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();

			final OutputStream stdin = process.getOutputStream();
			final InputStream stdout = process.getInputStream();
			final InputStream stderr = process.getErrorStream();

			final ExecutorService executor = Executors.newFixedThreadPool(2);

			executor.submit(() -> {
				try {
					consume(stdout, this.output);
				} catch (IOException e) {
					// Handle exception
				}
			});

			executor.submit(() -> {
				try {
					consume(stderr, this.error);
				} catch (IOException e) {
					// Handle exception
				}
			});

			if (stdinConsumer != null) {
				stdinConsumer.accept(stdin);
			}

			boolean finished = process.waitFor(10, TimeUnit.SECONDS);

			executor.shutdown();
		    executor.awaitTermination(5, TimeUnit.SECONDS);

			if (finished) {
				return process.exitValue();
			} else if (process.isAlive()) {
				process.destroyForcibly();
			}

			throw new InternalError("Process should not be running");
		}
	}

	public static class ChrootedScriptExecutor extends ProcessExecutor<ChrootedScriptExecutor> {

		public ChrootedScriptExecutor(File chrootDirectory, String scriptPath, String... arguments) {
			super(buildCommand(chrootDirectory, scriptPath, arguments));
		}

		public int executeScript(Consumer<OutputStream> stdinConsumer) throws IOException, InterruptedException {
			return run(stdinConsumer);
		}

		private static String[] buildCommand(File chrootDirectory, String scriptPath, String... arguments) {
			final List<String> command = new ArrayList<>();
			command.add("sudo");
			command.add("chroot");
			command.add(chrootDirectory.getAbsolutePath());
			command.add("/bin/sh");
			command.add(scriptPath);
			command.addAll(Arrays.asList(arguments));
			return command.toArray(String[]::new);
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

