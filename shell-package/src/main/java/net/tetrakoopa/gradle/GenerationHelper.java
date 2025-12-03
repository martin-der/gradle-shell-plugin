package net.tetrakoopa.gradle;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.function.UnaryOperator;

import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GenerationHelper {

	public static class PropertiesGenerator implements Closeable {

		private final Writer writer;

		private final Formatter formatter;

		@Setter
		@Accessors(fluent = true, chain = true)
		private UnaryOperator<String> renamer;

		public interface Formatter {
			void format(Writer writer, String name, String value, Class<?> valueClass) throws IOException;
		}

		private static final Formatter SIMPLE_KEY_VALUE_FORMATTER = (Writer writer, String name, String value, Class<?> valueClass) -> {

			writer
				.append(name)
				.append("=");
			if (value != null) {
				writer.append(value);
			};
		};

		public static final Formatter SHELL_VARIABLE_DECLARATION_FORMATTER = (Writer writer, String name, String value, Class<?> valueClass) -> {

			writer.append("declare -r");
			if (valueClass != null) {
				if (valueClass.equals(Integer.class)) {
					writer.append("i");
				}
			}
			writer
				.append(" ")
				.append(name)
				.append("=");
			if (value != null) {
				writer.append(value);
			};
		};


		public PropertiesGenerator(Writer writer) {
			this(writer, null);
		}
		public PropertiesGenerator(Writer writer, Formatter formatter) {
			this.writer = writer;
			this.formatter = formatter == null ? SIMPLE_KEY_VALUE_FORMATTER : formatter;
			this.renamer = null;
		}

		public void append(String name, int value) throws IOException {
			write(name, String.valueOf(value), Integer.class);
		}
		private void append(String name, String value) throws IOException {
			write(name, value, null);
		}
		public void append(String name, Object value) throws IOException {
			write(name, String.valueOf(value), value==null?null:value.getClass());
		}
		private void write(String name, String value, Class<?> valueClass) throws IOException {
			if (renamer != null) {
				name = renamer.apply(name);
			}
			formatter.format(writer, name, value, valueClass);
			writer.append('\n');
		}

		@Override
		public void close() throws IOException {
			writer.close();
		}
	}


}
