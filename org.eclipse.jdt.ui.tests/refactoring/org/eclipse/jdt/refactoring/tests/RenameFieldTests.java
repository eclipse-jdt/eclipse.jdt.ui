/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.*;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RenameFieldTests {
	
	private static final Class clazz= RenameFieldTests.class;
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), RenameFieldTests.class, args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(RenameNonPrivateFieldTests.noSetupSuite());
		suite.addTest(RenamePrivateFieldTests.noSetupSuite());
		return new MySetup(suite);
	}
}