package org.eclipse.jdt.ui.tests.nls;


import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class AllTests {
	
	public static void main (String[] args) {
		junit.textui.TestRunner.run (suite());
	}
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


