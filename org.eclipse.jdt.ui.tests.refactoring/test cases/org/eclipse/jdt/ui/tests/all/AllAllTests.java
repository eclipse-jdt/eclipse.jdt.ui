package org.eclipse.jdt.ui.tests.all;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllAllTests {
	public static Test suite ( ) {
		TestSuite suite= new TestSuite("All Tests");
		suite.addTest(org.eclipse.jdt.ui.tests.actions.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.nls.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.changes.AllTests.suite());
	    return suite;
	}
}

