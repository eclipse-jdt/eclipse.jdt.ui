/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.refactoring.tests;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.packages.RenamePackageRefactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RenamePackageTests extends RefactoringTest {
	
	private static final String REFACTORING_PATH= "RenamePackage/";

	public RenamePackageTests(String name) {
		super(name);
		//fgIsVerbose= true;
	}

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), RenamePackageTests.class, args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}

	public static Test noSetupSuite() {
		return new TestSuite(RenamePackageTests.class);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	// -------------
	private RenamePackageRefactoring createRefactoring(IPackageFragment pack, String newName) {
		RenamePackageRefactoring ref= new RenamePackageRefactoring(fgChangeCreator, pack);
		ref.setNewName(newName);
		return ref;
	}

	/* non java-doc
	 * the 0th one is the one to rename
	 */
	private void helper1(String packageNames[], String[][] packageFiles, String newPackageName) throws Exception{
		IPackageFragment[] packages= new IPackageFragment[packageNames.length];
		for (int i= 0; i < packageFiles.length; i++){
			packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
			for (int j= 0; j < packageFiles[i].length; j++){
				createCUfromTestFile(packages[i], packageFiles[i][j], packageNames[i].replace('.', '/') + "/");
				//DebugUtils.dump(cu.getElementName() + "\n" + cu.getSource());
			}	
		}
		IPackageFragment thisPackage= packages[0];
		IRefactoring ref= createRefactoring(thisPackage, newPackageName);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
		if (fIsVerbose)
			DebugUtils.dump("" + result);
	}
	
	/* non java-doc
	 * the 0th one is the one to rename
	 */
	private void helper1(String[] packageNames, String newPackageName) throws Exception{
		IPackageFragment[] packages= new IPackageFragment[packageNames.length];
		for (int i= 0; i < packageNames.length; i++){
			packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
		}
		IPackageFragment thisPackage= packages[0];
		IRefactoring ref= createRefactoring(thisPackage, newPackageName);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
		if (fIsVerbose)
			DebugUtils.dump("" + result);
	}
	
	private void helper1() throws Exception{
		helper1(new String[]{"p"}, new String[][]{{"A"}}, "p1");
	}
	
	private void helper2(String[] packageNames, String[][] packageFileNames, String newPackageName) throws Exception{
		IPackageFragment[] packages= new IPackageFragment[packageNames.length];
		ICompilationUnit[][] cus= new ICompilationUnit[packageFileNames.length][packageFileNames[0].length];
		for (int i= 0; i < packageNames.length; i++){
			packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
			for (int j= 0; j < packageFileNames[i].length; j++){
				cus[i][j]= createCUfromTestFile(packages[i], packageFileNames[i][j], packageNames[i].replace('.', '/') + "/");
			}
		}
		IPackageFragment thisPackage= packages[0];
		RefactoringStatus result= performRefactoring(createRefactoring(thisPackage, newPackageName));
		assertEquals("preconditions were supposed to pass", null, result);
		
		//---
		
		assert("package not renamed", ! getRoot().getPackageFragment(packageNames[0]).exists());
		IPackageFragment newPackage= getRoot().getPackageFragment(newPackageName);
		assert("new package does not exist", newPackage.exists());
		
		for (int i= 0; i < packageFileNames.length; i++){
			String packageName= (i == 0) 
							? newPackageName.replace('.', '/') + "/"
							: packageNames[i].replace('.', '/') + "/";
			for (int j= 0; j < packageFileNames[i].length; j++){
				String s1= getFileContents(getOutputTestFileName(packageFileNames[i][j], packageName));
				ICompilationUnit cu= 
					(i == 0) 
						? newPackage.getCompilationUnit(packageFileNames[i][j] + ".java")
						: cus[i][j];
				//DebugUtils.dump("cu:" + cu.getElementName());		
				String s2= cu.getSource();
				
				//DebugUtils.dump("expected:" + s1);
				//DebugUtils.dump("was:" + s2);
				assertEquals("invalid update in file " + cu.getElementName(), s1,	s2);
			}
		}
	}
	
	// ---------- tests -------------	
	public void testFail0() throws Exception{
		helper1(new String[]{"p"}, new String[][]{{"A"}}, "9");
	}
	
	public void testFail1() throws Exception{
		System.out.println("\nRenamePackageTest::" + name() + " disabled (needs revisiting)");
		//helper1(new String[]{"p.p1"}, new String[][]{{"A"}}, "p");
	}
	
	public void testFail2() throws Exception{
		helper1(new String[]{"p.p1", "fred"}, "fred");
	}	
	
	public void testFail3() throws Exception{
		helper1(new String[]{"p"}, new String[][]{{"A"}}, "fred");
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
		helper1(new String[]{"p", "p1"}, new String[][]{{"A"}, {"A"}}, "fred");
	}
	
	public void testFail8() throws Exception{
		helper1(new String[]{"p", "p1"}, new String[][]{{"A"}, {"A"}}, "fred");
	}
	
	//native method used p.A as a paramter
	public void testFail9() throws Exception{
		System.out.println("\nRenamePackageTest::" + name() + " disabled (corner case)");
		//helper1(new String[]{"p", "p1"}, new String[][]{{"A"}, {"A"}}, "fred");
	}
	
	public void testFail10() throws Exception{
		helper1(new String[]{"p.p1", "p"}, new String[][]{{"A"}, {"A"}}, "p");
	}

	public void testFail11() throws Exception{
		helper1(new String[]{"p.p1", "p", "r.p1"}, new String[][]{{"A"}, {"A"}, {}}, "r.p1");
	}
	
	//-------
	public void test0() throws Exception{
		helper2(new String[]{"p"}, new String[][]{{"A"}}, "p1");
	}
	
	public void test1() throws Exception{
		helper2(new String[]{"p"}, new String[][]{{"A"}}, "p1");
	}
	
	public void test2() throws Exception{
		helper2(new String[]{"p", "fred"}, new String[][]{{"A"}, {"A"}}, "p1");
	}
	
	public void test3() throws Exception{
		helper2(new String[]{"fred", "p.r"}, new String[][]{{"A"}, {"B"}}, "p");
	}
	
	public void test4() throws Exception{
		helper2(new String[]{"p.p1", "p"}, new String[][]{{"A"}, {"A"}}, "r");
	}
}
