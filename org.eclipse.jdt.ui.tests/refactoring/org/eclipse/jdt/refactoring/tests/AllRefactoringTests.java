/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;import junit.framework.TestSuite;import org.eclipse.jdt.testplugin.JavaTestSetup;import org.eclipse.jdt.testplugin.TestPluginLauncher;import org.eclipse.jdt.testplugin.ui.TestPluginUILauncher;

public class AllRefactoringTests {

	public static void main(String[] args) {
		TestPluginUILauncher.run(TestPluginLauncher.getLocationFromProperties(), AllRefactoringTests.class, args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite();
		
		//--code
		suite.addTest(ExtractMethodTests.noSetupSuite());
		
		//--methods
		suite.addTest(RenameVirtualMethodInClassTests.noSetupSuite());
		suite.addTest(RenameMethodInInterfaceTests.noSetupSuite());
		suite.addTest(RenamePrivateMethodTests.noSetupSuite());	
		suite.addTest(RenameStaticMethodTests.noSetupSuite());
		
		//--types
		suite.addTest(RenameTypeTests.noSetupSuite());	
		
		//--packages
		suite.addTest(RenamePackageTests.noSetupSuite());
		
		//--fields
		suite.addTest(RenamePrivateFieldTests.noSetupSuite());
		suite.addTest(RenameNonPrivateFieldTests.noSetupSuite());
		
		//--compilation units
		suite.addTest(MoveCUTests.noSetupSuite());
		
		//--other tests
		suite.addTest(UndoManagerTests.noSetupSuite());
		suite.addTest(PathTransformationTests.noSetupSuite());
				
		return new JavaTestSetup(suite);
	}
}
 
