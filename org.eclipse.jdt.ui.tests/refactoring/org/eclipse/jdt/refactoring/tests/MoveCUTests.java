/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.refactoring.IRefactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.cus.MoveCompilationUnitRefactoring;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class MoveCUTests extends RefactoringTest {

	private static final String REFACTORING_PATH= "MoveCU/";
	
	public MoveCUTests(String name) {
		super(name);
	}
	
	public static void main(String[] args) {
		args= new String[] { MoveCUTests.class.getName() };
		TestPluginLauncher.runUI(TestPluginLauncher.getLocationFromProperties(), args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}
	
	public static Test noSetupSuite() {
		return new TestSuite(MoveCUTests.class);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	// -------------
	private MoveCompilationUnitRefactoring createRefactoring(IJavaSearchScope scope, ICompilationUnit cu, IPackageFragment pack){
		return new MoveCompilationUnitRefactoring(fgChangeCreator, pack, cu);
	}
	
	private void helper1(String thisPackageName, String newPackageName, String[] thisPackageFiles, String[] newPackageFiles) throws Exception{
		IPackageFragment newPackage= getRoot().createPackageFragment(newPackageName, true, null);
		for (int i= 0; i < newPackageFiles.length; i++){
			createCUfromTestFile(newPackage, newPackageFiles[i], newPackageName.replace('.', '/') + "/");
		}
		for (int i= 0; i < thisPackageFiles.length; i++){
			createCUfromTestFile(getPackageP(), thisPackageFiles[i], thisPackageName.replace('.', '/') + "/");
		}
		ICompilationUnit cu= getPackageP().getCompilationUnit(thisPackageFiles[0] + ".java");
		IRefactoring ref= createRefactoring(getScope(), cu, newPackage);
		assertNotNull("precondition was supposed to fail", performRefactoring(ref));
	}
	
	private void helper1() throws Exception{
		helper1("p", "p1", new String[]{"A"}, new String[]{"C"});
	}
	
	private void helper2(String thisPackageName, String newPackageName, String[] thisPackageFiles, String[] newPackageFiles) throws Exception{
		IPackageFragment newPackage= getRoot().createPackageFragment(newPackageName, true, null);
		ICompilationUnit[] newPackageCUs= new ICompilationUnit[newPackageFiles.length];
		for (int i= 0; i < newPackageFiles.length; i++){
			newPackageCUs[i]= createCUfromTestFile(newPackage, newPackageFiles[i], newPackageName.replace('.', '/') + "/");
		}
		
		ICompilationUnit[] thisPackageCUs= new ICompilationUnit[thisPackageFiles.length];
		for (int i= 0; i < thisPackageFiles.length; i++){
			thisPackageCUs[i]= createCUfromTestFile(getPackageP(), thisPackageFiles[i], thisPackageName.replace('.', '/') + "/");
		}
		
		IPackageFragment pack= (IPackageFragment)thisPackageCUs[0].getParent();
		IPackageFragment newPack= (IPackageFragment)newPackageCUs[0].getParent();
		IRefactoring ref= createRefactoring(getScope(), thisPackageCUs[0], newPackage);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("preconditions were supposed to pass", null, result);
		assert("file not moved from", ! pack.getCompilationUnit(thisPackageFiles[0] + ".java").exists());
		ICompilationUnit movedCu= newPack.getCompilationUnit(thisPackageFiles[0] + ".java");
		assert("file not moved to", movedCu.exists());
		
		for (int i= 0; i < newPackageFiles.length; i++){
			assertEquals("invalid update", getFileContents(getOutputTestFileName(newPackageFiles[i], newPackageName.replace('.', '/') + "/")),
					 newPackageCUs[i].getSource());
		}
		assertEquals("invalid update in moved file", getFileContents(getOutputTestFileName(thisPackageFiles[0], newPackageName.replace('.', '/') + "/")),
					 movedCu.getSource());
	}
	
	private void helper2() throws Exception{
		helper2("p", "p1", new String[]{"A"}, new String[]{"C"});
	}
	
	// -------- tests
	public void testFail0() throws Exception{
		helper1("p", "p1", new String[]{"A"}, new String[]{"A"});
	}
	
	public void testFail1() throws Exception{
		helper1("p", "p1", new String[]{"A"}, new String[]{"A"});
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
		helper1("p", "p1", new String[]{"A", "B"}, new String[]{"C"});
	}
	
	public void testFail7() throws Exception{
		helper1("p", "p1", new String[]{"A", "B"}, new String[]{"C"});
	}
	
	public void testFail8() throws Exception{
		helper1("p", "p1", new String[]{"A", "B"}, new String[]{"B"});
	}
	
	public void testFail9() throws Exception{
		helper1("p", "p1", new String[]{"A", "B"}, new String[]{"C"});
	}
	
	// ----
	public void test0() throws Exception{
		helper2();
	}
	
	public void test1() throws Exception{
		helper2("p", "p1", new String[]{"A", "B"}, new String[]{"C"});
	}
	
	public void test2() throws Exception{
		helper2("p", "p1", new String[]{"A", "B"}, new String[]{"C"});
	}
	
}