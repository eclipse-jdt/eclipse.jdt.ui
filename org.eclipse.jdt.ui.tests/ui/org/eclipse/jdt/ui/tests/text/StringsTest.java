package org.eclipse.jdt.ui.tests.text;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.util.Strings;

public class StringsTest extends TestCase {


	public StringsTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(StringsTest.class);
	}

	
	public void testIsIndentChar() {
		assertTrue(Strings.isIndentChar(' '));
		assertTrue(Strings.isIndentChar('\t'));
		assertTrue(!Strings.isIndentChar('x'));
		assertTrue(!Strings.isIndentChar('\n'));
		assertTrue(!Strings.isIndentChar('\r'));
	}
		
	public void testIsLineDelimiterChar() {
		assertTrue(!Strings.isLineDelimiterChar(' '));
		assertTrue(!Strings.isLineDelimiterChar('\t'));
		assertTrue(!Strings.isLineDelimiterChar('x'));
		assertTrue(Strings.isLineDelimiterChar('\n'));
		assertTrue(Strings.isLineDelimiterChar('\r'));
	}
	
}

