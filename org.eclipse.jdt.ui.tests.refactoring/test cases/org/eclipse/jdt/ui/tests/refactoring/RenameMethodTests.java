/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RenameMethodTests {
	private static final Class clazz= RenameMethodTests.class;

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(RenameVirtualMethodInClassTests.suite());
		suite.addTest(RenameMethodInInterfaceTests.suite());
		suite.addTest(RenamePrivateMethodTests.suite());	
		suite.addTest(RenameStaticMethodTests.suite());
		return suite;
	}
}