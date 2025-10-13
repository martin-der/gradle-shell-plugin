package net.tetrakoopa.gradle.plugin.usual;

import java.util.function.UnaryOperator;


public class NameMapper {

	public static final UnaryOperator<String> REMOVE_SUFFIX = s -> s.replaceFirst("^(.*)\\..+$", "$1");

	public static final UnaryOperator<String> TO_LOWER_CASE = s -> s.toLowerCase();
}
