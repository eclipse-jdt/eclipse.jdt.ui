/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;import junit.framework.TestSuite;import org.eclipse.jdt.testplugin.JavaTestSetup;import org.eclipse.jdt.testplugin.TestPluginLauncher;import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.*;


public class AllRefactoringTests {

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), AllRefactoringTests.class, args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());						
		return new JavaTestSetup(suite);
	}
	
	public static Test noSetupSuite() {
		TestSuite suite= new TestSuite();
		
		//--code
		suite.addTest(ExtractMethodTests.noSetupSuite());
		
		//--methods
		suite.addTest(RenameVirtualMethodInClassTests.noSetupSuite());
		suite.addTest(RenameMethodInInterfaceTests.noSetupSuite());
		suite.addTest(RenamePrivateMethodTests.noSetupSuite());	
		suite.addTest(RenameStaticMethodTests.noSetupSuite());
		suite.addTest(RenameParametersTests.noSetupSuite());
		suite.addTest(ReorderParametersTests.noSetupSuite());
		
		//--types
		suite.addTest(RenameTypeTests.noSetupSuite());	
		
		//--packages
		suite.addTest(RenamePackageTests.noSetupSuite());
		
		//--fields
		suite.addTest(RenamePrivateFieldTests.noSetupSuite());
		suite.addTest(RenameNonPrivateFieldTests.noSetupSuite());
		
		//--compilation units
		suite.addTest(MoveCUTests.noSetupSuite());

		return suite;
	}
	
}
 
