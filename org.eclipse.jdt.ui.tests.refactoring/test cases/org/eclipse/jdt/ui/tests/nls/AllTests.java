package org.eclipse.jdt.ui.tests.nls;


import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {
	
	public static Test suite ( ) {
		TestSuite suite= new TestSuite("All NLS Tests");
		suite.addTest(NLSElementTester.suite());
		suite.addTest(NLSScannerTester.suite());
		suite.addTest(NLSRefactoringTester.suite());
		suite.addTest(CellEditorTester.suite());
		suite.addTest(OrderedMapTester.suite());
	    return suite;
	}
}


