/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllRefactoringTests {

	private static final Class clazz= AllRefactoringTests.class;

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());

		//--code
		suite.addTest(ExtractMethodTests.suite());
		suite.addTest(InlineTempTests.suite());

		//--methods
		suite.addTest(RenameVirtualMethodInClassTests.suite());
		suite.addTest(RenameMethodInInterfaceTests.suite());
		suite.addTest(RenamePrivateMethodTests.suite());	
		suite.addTest(RenameStaticMethodTests.suite());
		suite.addTest(RenameParametersTests.suite());
		suite.addTest(RenameTempTests.suite());
		//suite.addTest(ReorderParametersTests.noSetupSuite());
		
		//--types
		suite.addTest(RenameTypeTests.suite());	
		
		//--packages
		suite.addTest(RenamePackageTests.suite());
		
		//--fields
		suite.addTest(RenamePrivateFieldTests.suite());
		suite.addTest(RenameNonPrivateFieldTests.suite());
		
		//--compilation units
		//suite.addTest(MoveCUTests.noSetupSuite());
		suite.addTest(MultiMoveTests.suite());

		//--projects
		suite.addTest(RenameJavaProjectTests.suite());		
		return suite;
	}
}
 
