package org.eclipse.jdt.ui.tests.actions;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	
	public static Test suite ( ) {
		TestSuite suite= new TestSuite("All Action Tests");
		suite.addTest(DeleteSourceReferenceEditTests.suite());
		suite.addTest(PasteSourceReferenceActionTests.suite());
		suite.addTest(DeleteSourceReferenceActionTests.suite());
		suite.addTest(StructureSelectionActionTests.suite());
	    return suite;
	}

}


