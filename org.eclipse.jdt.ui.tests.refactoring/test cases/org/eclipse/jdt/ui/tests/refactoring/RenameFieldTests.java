/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RenameFieldTests {
	
	private static final Class clazz= RenameFieldTests.class;
	
	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(RenameNonPrivateFieldTests.suite());
		suite.addTest(RenamePrivateFieldTests.suite());
		return new MySetup(suite);
	}
}