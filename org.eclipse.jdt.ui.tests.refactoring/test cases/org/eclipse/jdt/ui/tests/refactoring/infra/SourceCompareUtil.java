package org.eclipse.jdt.ui.tests.refactoring.infra;

import junit.framework.Assert;

import org.eclipse.jdt.internal.corext.util.Strings;

public class SourceCompareUtil extends Assert{

    private SourceCompareUtil() {}

	public static void compare(String actual, String expected) {
		compare("", actual, expected);
	}

	public static void compare(String message, String actual, String expected) {
		String[] actualCode= Strings.convertIntoLines(actual);
		String[] expectedCode= Strings.convertIntoLines(expected);
		if(expectedCode.length != actualCode.length){
			assertEquals(message + " Different number of lines (" + actualCode.length + " not " + expectedCode.length+")" , expected, actual);
			return;
		}
		for (int i= 0; i < expectedCode.length; i++) {
			assertEquals(message + " Difference in line " + (i+1) , expectedCode[i], actualCode[i]);
		}
	}
}