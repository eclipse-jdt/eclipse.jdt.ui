package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {
	
	private static final Class clazz= AllTests.class;
	
	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(AllRefactoringTests.noSetupSuite());
		suite.addTest(ReorgTests.noSetupSuite());
		suite.addTest(AllChangeTests.noSetupSuite());
		suite.addTest(UndoManagerTests.noSetupSuite());
		suite.addTest(PathTransformationTests.noSetupSuite());
		suite.addTest(RefactoringScannerTests.noSetupSuite());
		return new MySetup(suite);
	}
}

