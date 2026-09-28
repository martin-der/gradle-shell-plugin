package net.tetrakoopa.gradle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the quoting rule that {@code AbstractShellPackagePluginFunctionalTest#asRenderedInScript}
 * mirrors by hand (the functional test source set cannot see plugin classes).
 */
public class ShellEscaperTest {

	@Test
	public void leavesSafeValuesUnquoted() {
		assertEquals("foobar", ShellEscaper.quote("foobar"));
		assertEquals("1.2.3-buggy-prealpha", ShellEscaper.quote("1.2.3-buggy-prealpha"));
		assertEquals("mdu-shell-dispenser__foobar__0123456789abcdef", ShellEscaper.quote("mdu-shell-dispenser__foobar__0123456789abcdef"));
		assertEquals("/usr/local/bin", ShellEscaper.quote("/usr/local/bin"));
	}

	@Test
	public void quotesAnythingWithASpace() {
		assertEquals("'Foo Bar Frenzy'", ShellEscaper.quote("Foo Bar Frenzy"));
	}

	@Test
	public void quotesValuesThatWouldOtherwiseBeGlitched() {
		assertEquals("''", ShellEscaper.quote(""));
		assertEquals("'; rm -rf /'", ShellEscaper.quote("; rm -rf /"));
		assertEquals("'$HOME'", ShellEscaper.quote("$HOME"));
		assertEquals("'*'", ShellEscaper.quote("*"));
		assertEquals("a=b", ShellEscaper.quote("a=b"));
		assertEquals("'back\\slash'", ShellEscaper.quote("back\\slash"));
	}

	@Test
	public void closesAndReopensTheQuoteForAnEmbeddedSingleQuote() {
		assertEquals("'it'\\''s'", ShellEscaper.quote("it's"));
		assertEquals("''\\'''", ShellEscaper.quote("'"));
	}

	@Test
	public void passesNullThrough() {
		assertNull(ShellEscaper.quote(null));
	}

	@Test
	public void isSafeUnquotedExcludesEmptyAndNull() {
		assertFalse(ShellEscaper.isSafeUnquoted(null));
		assertFalse(ShellEscaper.isSafeUnquoted(""));
		assertTrue(ShellEscaper.isSafeUnquoted("a"));
	}

	@Test
	public void escapesTheFiveCharactersThatMatterInsideDoubleQuotes() {
		assertEquals("a\\\"b", ShellEscaper.escapeForDoubleQuotes("a\"b"));
		assertEquals("a\\$b", ShellEscaper.escapeForDoubleQuotes("a$b"));
		assertEquals("a\\`b", ShellEscaper.escapeForDoubleQuotes("a`b"));
		assertEquals("a\\\\b", ShellEscaper.escapeForDoubleQuotes("a\\b"));
		assertEquals("a'b", ShellEscaper.escapeForDoubleQuotes("a'b"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void refusesToEmbedALineBreakInASingleShellWord() {
		ShellEscaper.escapeForDoubleQuotes("a\nb");
	}
}
