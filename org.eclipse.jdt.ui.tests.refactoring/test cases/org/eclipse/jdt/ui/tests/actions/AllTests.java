package org.eclipse.jdt.ui.tests.actions;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class AllTests {
	
	public static Test suite ( ) {
		TestSuite suite= new TestSuite("All Actions Tests");
		suite.addTest(PasteSourceReferenceActionTests.suite());
	    return suite;
	}

}


