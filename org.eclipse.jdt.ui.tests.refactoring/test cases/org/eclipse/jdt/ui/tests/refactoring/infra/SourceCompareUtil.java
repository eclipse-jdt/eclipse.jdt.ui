package org.eclipse.jdt.ui.tests.refactoring.infra;

import junit.framework.Assert;

import org.eclipse.jdt.internal.corext.util.Strings;

public class SourceCompareUtil extends Assert{

    private SourceCompareUtil() {}

	public static void compare(String actual, String expected) {
		String[] actualCode= Strings.convertIntoLines(actual);
		String[] expectedCode= Strings.convertIntoLines(expected);
		if(expectedCode.length != actualCode.length){
			assertEquals("Different number of lines (" + actualCode.length + " not " + expectedCode.length+")" , expected, actual);
			return;
		}
		for (int i= 0; i < expectedCode.length; i++) {
			assertEquals("Difference in line " + (i+1) , expectedCode[i], actualCode[i]);
		}
	}
}