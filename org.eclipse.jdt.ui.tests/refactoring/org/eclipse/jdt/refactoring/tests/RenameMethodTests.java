/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.ui.*;

public class RenameMethodTests {
	
	public static void main(String[] args) {
		TestPluginUILauncher.run(TestPluginLauncher.getLocationFromProperties(), RenameMethodTests.class, args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(RenameVirtualMethodInClassTests.noSetupSuite());
		suite.addTest(RenameMethodInInterfaceTests.noSetupSuite());
		suite.addTest(RenamePrivateMethodTests.noSetupSuite());	
		suite.addTest(RenameStaticMethodTests.noSetupSuite());
		return new JavaTestSetup(suite);
	}
}