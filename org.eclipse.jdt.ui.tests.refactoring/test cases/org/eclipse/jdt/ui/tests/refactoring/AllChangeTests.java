package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllChangeTests {
	private static final Class clazz= AllChangeTests.class;

	public static Test noSetupSuite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(new TestSuite(RenameResourceChangeTests.class));
		suite.addTest(new TestSuite(RenameSourceFolderChangeTests.class));
		suite.addTest(new TestSuite(CopyPackageChangeTest.class));
		return suite;
	}

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(noSetupSuite());
		return new MySetup(suite);
	}
	
}

