package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	
	private static final Class clazz= AllTests.class;
	
	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(AllRefactoringTests.suite());
		suite.addTest(ReorgTests.suite());
		suite.addTest(AllChangeTests.suite());
		suite.addTest(UndoManagerTests.suite());
		suite.addTest(PathTransformationTests.suite());
		suite.addTest(RefactoringScannerTests.suite());
		suite.addTest(SelectionAnalyzerTests.suite());
		return suite;
	}
}

