package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.*;


public class AllChangeTests {
	private static final Class clazz= AllChangeTests.class;

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), clazz, args);
	}
	
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

