package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.ui.TestPluginUILauncher;

public class AllTests {
	public static void main(String[] args) {
		TestPluginUILauncher.run(TestPluginLauncher.getLocationFromProperties(), AllTests.class, args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(AllRefactoringTests.noSetupSuite());
		suite.addTest(AllChangeTests.noSetupSuite());
		suite.addTest(UndoManagerTests.noSetupSuite());
		suite.addTest(PathTransformationTests.noSetupSuite());
		return new JavaTestSetup(suite);
	}
}

