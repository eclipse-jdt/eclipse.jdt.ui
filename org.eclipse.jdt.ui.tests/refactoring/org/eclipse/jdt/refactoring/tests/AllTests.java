package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.*;


public class AllTests {
	
	private static final Class clazz= AllTests.class;
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), clazz, args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(AllRefactoringTests.noSetupSuite());
		suite.addTest(AllChangeTests.noSetupSuite());
		suite.addTest(UndoManagerTests.noSetupSuite());
		suite.addTest(PathTransformationTests.noSetupSuite());
		return new MySetup(suite);
	}
}

