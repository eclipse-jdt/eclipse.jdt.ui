/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class RenameFieldTests {
	
	public static void main(String[] args) {
		args= new String[] { RenameFieldTests.class.getName() };
		TestPluginLauncher.runUI(TestPluginLauncher.getLocationFromProperties(), args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(RenameNonPrivateFieldTests.noSetupSuite());
		suite.addTest(RenamePrivateFieldTests.noSetupSuite());
		return new JavaTestSetup(suite);
	}
}