/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.IRefactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.types.SafeDeleteTypeRefactoring;

import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class SafeDeleteTypeTests extends RefactoringTest {

	private static final String REFACTORING_PATH= "SafeDeleteType/";
	
	public SafeDeleteTypeTests(String name) {
		super(name);
	}

	public static void main(String[] args) {
		args= new String[] { SafeDeleteTypeTests.class.getName() };
		TestPluginLauncher.runUI(TestPluginLauncher.getLocationFromProperties(), args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}

	public static Test noSetupSuite() {
		return new TestSuite(SafeDeleteTypeTests.class);
	}
	
	protected String getRefactoringPath(){
		return REFACTORING_PATH;
	}
	
	private void helper1_0(String name) throws Exception{
		IType type= getType(createCUfromTestFile(getPackageP(), "A"), name);
		IRefactoring ref= new SafeDeleteTypeRefactoring(fgChangeCreator, getScope(), type);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
	}	
	
	private void helper1() throws Exception{
		helper1_0("A");
	}
	
	public void testFail0() throws Exception{
		helper1();
	}

	public void testFail1() throws Exception{
		helper1();
	}

	public void testFail2() throws Exception{
		helper1();
	}

	public void testFail3() throws Exception{
		helper1();
	}

	public void testFail4() throws Exception{
		helper1();
	}

	public void testFail5() throws Exception{
		helper1();
	}

	public void testFail6() throws Exception{
		helper1();
	}

	public void testFail7() throws Exception{
		helper1();
	}

	public void testFail8() throws Exception{
		helper1();
	}

	public void testFail9() throws Exception{
		helper1();
	}

	public void testFail10() throws Exception{
		helper1();
	}

	public void testFail11() throws Exception{
		helper1();
	}

	public void testFail12() throws Exception{
		helper1();
	}

	public void testFail13() throws Exception{
		helper1();
	}

	public void testFail14() throws Exception{
		helper1();
	}

	public void testFail15() throws Exception{
		helper1();
	}

	public void testFail16() throws Exception{
		helper1();
	}

	public void testFail17() throws Exception{
		helper1();
	}

	public void testFail18() throws Exception{
		helper1();
	}

	/*public void testFail19() throws Exception{
		helper1();
	}*/

	public void testFail20() throws Exception{
		helper1();
	}

	public void testFail21() throws Exception{
		helper1();
	}	
}
