package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.ui.TestPluginUILauncher;

public class AllChangeTests {

	public static void main(String[] args) {
		TestPluginUILauncher.run(TestPluginLauncher.getLocationFromProperties(), AllChangeTests.class, args);
	}
	
	public static Test noSetupSuite() {
		TestSuite suite= new TestSuite();
		suite.addTest(new TestSuite(RenameResourceChangeTests.class));
		return suite;
	}

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}
	
}

