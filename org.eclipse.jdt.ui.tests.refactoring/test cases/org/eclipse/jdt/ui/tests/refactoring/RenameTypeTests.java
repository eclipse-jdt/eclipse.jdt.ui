/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.ui.tests.refactoring.infra.DebugUtils;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameTypeTests extends RefactoringTest {
	
	private static final Class clazz= RenameTypeTests.class;
	private static final String REFACTORING_PATH= "RenameType/";
	
	public RenameTypeTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	public static Test setUpTest(Test someTest) {
		return new MySetup(someTest);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
		
	/******* shortcuts **********/
	
	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, className), className);
	}
		
	private RenameRefactoring createRefactoring(IType type, String newName) throws CoreException {
		RenameTypeProcessor processor= new RenameTypeProcessor(type);
		RenameRefactoring ref= new RenameRefactoring(processor);
		processor.setNewElementName(newName);
		return ref;
	}
	
	private void helper1_0(String className, String newName) throws Exception{
		IType classA= getClassFromTestFile(getPackageP(), className);
		Refactoring ref= createRefactoring(classA, newName);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
		if (fIsVerbose)
			DebugUtils.dump("result: " + result);
	}
	
	private void helper1() throws Exception{
		helper1_0("A", "B");
	}
		
	private String[] helperWithTextual(String oldCuName, String oldName, String newName, String newCUName, boolean updateReferences, boolean updateTextualMatches) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), oldCuName);
		IType classA= getType(cu, oldName);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String[] renameHandles= null;
		if (classA.getDeclaringType() == null && cu.getElementName().startsWith(classA.getElementName())) {
			renameHandles= ParticipantTesting.createHandles(classA, cu, cu.getResource());
		} else {
			renameHandles= ParticipantTesting.createHandles(classA);
		}
		RenameRefactoring ref= createRefactoring(classA, newName);
		RenameTypeProcessor processor= (RenameTypeProcessor)ref.getProcessor();
		processor.setUpdateReferences(updateReferences);
		processor.setUpdateTextualMatches(updateTextualMatches);
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		ICompilationUnit newcu= pack.getCompilationUnit(newCUName + ".java");
		assertTrue("cu " + newcu.getElementName()+ " does not exist", newcu.exists());
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName(newCUName)), newcu.getSource());
		return renameHandles;
	}
	
	private String[] helper2_0(String oldName, String newName, String newCUName, boolean updateReferences) throws Exception{
		return helperWithTextual(oldName, oldName, newName, newCUName, updateReferences, false);
	}
	
	private void helper2(String oldName, String newName, boolean updateReferences) throws Exception{
		helper2_0(oldName, newName, newName, updateReferences);
	}

	private String[] helper2(String oldName, String newName) throws Exception{
		return helper2_0(oldName, newName, newName, true);
	}
				
	/****** tests ***********/
	
	public void testIllegalInnerClass() throws Exception {
		helper1();
	}
	
	public void testIllegalTypeName1() throws Exception {
		helper1_0("A", "X ");
	}
	
	public void testIllegalTypeName2() throws Exception {
		helper1_0("A", " X");
	}
	
	public void testIllegalTypeName3() throws Exception {
		helper1_0("A", "34");
	}

	public void testIllegalTypeName4() throws Exception {
		helper1_0("A", "this");
	}

	public void testIllegalTypeName5() throws Exception {
		helper1_0("A", "fred");
	}
	
	public void testIllegalTypeName6() throws Exception {
		helper1_0("A", "class");
	}
	
	public void testIllegalTypeName7() throws Exception {
		helper1_0("A", "A.B");
	}

	public void testIllegalTypeName8() throws Exception {
		helper1_0("A", "A$B");
	}
	
	public void testIllegalTypeName9() throws Exception {
		if (BootLoader.getOS().equals(BootLoader.OS_WIN32))
			helper1_0("A", "aux");
	}

	public void testNoOp() throws Exception {
		helper1_0("A", "A");
	}

	public void testWrongArg1() throws Exception {
		helper1_0("A", "");
	}
	
	public void testFail0() throws Exception {
		helper1();
	}
	
	public void testFail1() throws Exception {
		helper1();
	}

	public void testFail2() throws Exception {
		helper1();
	}
	
	public void testFail3() throws Exception {
		helper1();
	}
	
	public void testFail4() throws Exception {
		IType classA= getClassFromTestFile(getPackageP(), "A");
		getClassFromTestFile(getPackageP(), "B");
		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}
		
	public void testFail5() throws Exception {
		IType classA= getClassFromTestFile(getPackageP(), "A");
		getClassFromTestFile(getPackageP(), "B");
		getClassFromTestFile(getPackageP(), "C");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}

	public void testFail6() throws Exception {
		IType classA= getClassFromTestFile(getPackageP(), "A");
		getClassFromTestFile(getPackageP(), "B");
		getClassFromTestFile(getPackageP(), "C");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}
	
	public void testFail7() throws Exception {
		helper1();
	}
	
	public void testFail8() throws Exception {
		IType classA= getClassFromTestFile(getPackageP(), "A");
		getClassFromTestFile(getPackageP(), "B");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}
	
	public void testFail9() throws Exception {
		helper1();
	}
	
	public void testFail10() throws Exception {
		helper1();
	}

	public void testFail11() throws Exception {
		helper1();
	}

	public void testFail12() throws Exception {
		helper1();
	}

	public void testFail16() throws Exception {
		helper1();
	}

	public void testFail17() throws Exception {
		helper1();
	}

	public void testFail18() throws Exception {
		helper1();
	}

	public void testFail19() throws Exception {
		helper1();
	}

	public void testFail20() throws Exception {
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		IPackageFragment packageP2= getRoot().createPackageFragment("p2", true, null);

		IType classA= getClassFromTestFile(packageP1, "A");
		getClassFromTestFile(packageP2, "AA");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}

	public void testFail22() throws Exception {
		IType classA= getClassFromTestFile(getPackageP(), "A");
		getClassFromTestFile(getPackageP(), "B");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}

	public void testFail23() throws Exception {
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		IPackageFragment packageP2= getRoot().createPackageFragment("p2", true, null);
		IPackageFragment packageP3= getRoot().createPackageFragment("p3", true, null);

		IType classA= getClassFromTestFile(packageP1, "A");
		getClassFromTestFile(packageP3, "B");
		getClassFromTestFile(packageP2, "Bogus");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}

	public void testFail24() throws Exception {
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		IPackageFragment packageP2= getRoot().createPackageFragment("p2", true, null);

		IType classA= getClassFromTestFile(packageP1, "A");
		getClassFromTestFile(packageP2, "B");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}

	public void testFail25() throws Exception {
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		IPackageFragment packageP2= getRoot().createPackageFragment("p2", true, null);

		IType classA= getClassFromTestFile(packageP1, "A");
		getClassFromTestFile(packageP2, "B");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}

	public void testFail26() throws Exception {
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		IPackageFragment packageP2= getRoot().createPackageFragment("p2", true, null);

		IType classA= getClassFromTestFile(packageP1, "A");
		getClassFromTestFile(packageP2, "B");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}

	public void testFail27() throws Exception {
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		IPackageFragment packageP2= getRoot().createPackageFragment("p2", true, null);

		IType classA= getClassFromTestFile(packageP1, "A");
		getClassFromTestFile(packageP2, "B");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}
	
	public void testFail28() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail29() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail30() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail31() throws Exception {
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		IPackageFragment packageP2= getRoot().createPackageFragment("p2", true, null);
		IPackageFragment packageP3= getRoot().createPackageFragment("p3", true, null);

		IType classA= getClassFromTestFile(packageP1, "A");
		getClassFromTestFile(packageP2, "B");
		getClassFromTestFile(packageP3, "C");

		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}

	public void testFail32() throws Exception {
		IType classA= getClassFromTestFile(getPackageP(), "A");
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		getClassFromTestFile(packageP1, "B");
		
		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}
	
	public void testFail33() throws Exception {
		helper1();
	}

	public void testFail34() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail35() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail36() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
		
	public void testFail37() throws Exception {
		IType classA= getClassFromTestFile(getPackageP(), "A");
		getClassFromTestFile(getPackageP(), "B");
		
		RefactoringStatus result= performRefactoring(createRefactoring(classA, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}
	
	public void testFail38() throws Exception {
		helper1();
	}
	
	public void testFail39() throws Exception {
		helper1();
	}
	
	public void testFail40() throws Exception {
		helper1();
	}
	
	public void testFail41() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail42() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}

	public void testFail43() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}

	public void testFail44() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail45() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail46() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail47() throws Exception {
		printTestDisabledMessage("obscuring");
//		helper1();
	}
	
	public void testFail48() throws Exception {
		helper1();
	}
	
	public void testFail49() throws Exception {
		helper1();
	}
	
	public void testFail50() throws Exception {
		helper1();
	}

	public void testFail51() throws Exception {
		helper1();
	}

	public void testFail52() throws Exception {
		helper1();
	}
	
	public void testFail53() throws Exception {
		helper1();
	}
	
	public void testFail54() throws Exception {
		helper1();
	}
	
	public void testFail55() throws Exception {
		helper1();
	}
	
	public void testFail56() throws Exception {
		helper1();
	}
	
	public void testFail57() throws Exception {
		helper1();
	}
	
	public void testFail58() throws Exception {
		helper1();
	}

	public void testFail59() throws Exception {
		helper1();
	}

	public void testFail60() throws Exception {
		helper1();
	}
	
	public void testFail61() throws Exception {
		helper1();
	}
	
	public void testFail62() throws Exception {
		helper1();
	}
	
	public void testFail63() throws Exception {
		helper1();
	}
	
	public void testFail64() throws Exception {
		helper1();
	}
	
	public void testFail65() throws Exception {
		helper1();
	}
	
	public void testFail66() throws Exception {
		helper1();
	}

	public void testFail67() throws Exception {
		helper1();
	}

	public void testFail68() throws Exception {
		helper1();
	}
	
	public void testFail69() throws Exception {
		helper1();
	}
	
	public void testFail70() throws Exception {
		helper1();
	}
	
	public void testFail71() throws Exception {
		helper1();
	}
	
	public void testFail72() throws Exception {
		helper1();
	}
	
	public void testFail73() throws Exception {
		helper1();
	}
	
	public void testFail74() throws Exception {
		helper1();
	}
	
	public void testFail75() throws Exception {
		helper1();
	}
	
	public void testFail76() throws Exception {
		helper1();
	}

	public void testFail77() throws Exception {
		helper1();
	}

	public void testFail78() throws Exception {
		helper1();
	}
	
	public void testFail79() throws Exception {
		helper1();
	}

	public void testFail80() throws Exception {
		helper1();
	}
	
	public void testFail81() throws Exception {
		helper1();
	}
	
	public void testFail82() throws Exception {
		helper1();
	}
	
	public void testFail83() throws Exception {
		helper1_0("A", "Cloneable");
	}
	
	public void testFail84() throws Exception {
		helper1_0("A", "List");
	}
	
	public void testFail85() throws Exception {
		helper1();
	}
	
	public void testFail86() throws Exception {
		printTestDisabledMessage("native method with A as parameter (same CU)");
//		helper1();
	}

	public void testFail87() throws Exception {
		printTestDisabledMessage("native method with A as parameter (same CU)");
//		helper1();
	}
	
	public void testFail88() throws Exception {
		helper1();
	}
	
	public void testFail89() throws Exception {
		helper1();
	}
	
	public void testFail90() throws Exception {
		helper1();
	}
	
	public void testFail91() throws Exception {
		helper1();
	}

	public void testFail92() throws Exception {
//		printTestDisabledMessage("needs fixing - double nested local type named B");
		helper1();
	}

	public void testFail93() throws Exception {
//		printTestDisabledMessage("needs fixing - double nested local type named B");
		helper1();
	}
	
	public void testFail94() throws Exception {
		helper1();
	}
	
	public void testFail95() throws Exception {
		helper1();
	}

	public void testFail00() throws Exception {
		helper1();
	}
	
	public void testFail01() throws Exception {
		helper1_0("A", "B");
	}

	public void testFail02() throws Exception {
		helper1();
	}

	public void testFail03() throws Exception {
		helper1_0("A", "C");
	}

	public void testFail04() throws Exception {
		helper1_0("A", "A");
	}
	
	public void testFailRegression1GCRKMQ() throws Exception {
		IPackageFragment myPackage= getRoot().createPackageFragment("", true, new NullProgressMonitor());
		IType myClass= getClassFromTestFile(myPackage, "Blinky");
		
		RefactoringStatus result= performRefactoring(createRefactoring(myClass, "B"));
		assertNotNull("precondition was supposed to fail", result);
	}
	
	/******************/
	
	public void test0() throws Exception { 
		ParticipantTesting.reset();
		String newName= "B";
		String[] renameHandles= helper2("A", newName);
		ParticipantTesting.testRename(
			renameHandles,
			new RenameArguments[] {
				new RenameArguments(newName, true), 
				new RenameArguments(newName + ".java", true),
				new RenameArguments(newName + ".java", true)});
	}
	
	public void test1() throws Exception { 
		helper2("A", "B");
	}
	
	public void test10() throws Exception { 
		helper2("A", "B");
	}
	
	public void test12() throws Exception { 
		helper2("A", "B");
	}
	
	public void test13() throws Exception { 
		helper2("A", "B");
	}
	
	public void test14() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test15() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test16() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test17() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test18() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test19() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test2() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test20() throws Exception { 
		//printTestDisabledMessage("failb because of bug#9479");
		//if (true)
		//	return;
		IPackageFragment packageA= getRoot().createPackageFragment("A", true, null);
		
		ICompilationUnit cu= createCUfromTestFile(packageA, "A");
		IType classA= getType(cu, "A");
		
		Refactoring ref= createRefactoring(classA, "B");
		
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		
		ICompilationUnit newcu= packageA.getCompilationUnit("B.java");
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), newcu.getSource());
	}
	
	public void test21() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test22() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test23() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test24() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test25() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test26() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test27() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test28() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test29() throws Exception { 
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		createCUfromTestFile(packageP1, "C");
		
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
				
		Refactoring ref= createRefactoring(classA, "B");
		
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		
		ICompilationUnit newcu= getPackageP().getCompilationUnit("B.java");
		ICompilationUnit newcuC= packageP1.getCompilationUnit("C.java");
		assertEqualLines("invalid renaming A", getFileContents(getOutputTestFileName("B")), newcu.getSource());
		assertEqualLines("invalid renaming C", getFileContents(getOutputTestFileName("C")), newcuC.getSource());		
		
	}
	
	public void test3() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test30() throws Exception { 
		createCUfromTestFile(getPackageP(), "AA");
		
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
				
		Refactoring ref= createRefactoring(classA, "B");
		
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		
		ICompilationUnit newcu= getPackageP().getCompilationUnit("B.java");
		ICompilationUnit newcuAA= getPackageP().getCompilationUnit("AA.java");
		assertEqualLines("invalid renaming A", getFileContents(getOutputTestFileName("B")), newcu.getSource());
		assertEqualLines("invalid renaming AA", getFileContents(getOutputTestFileName("AA")), newcuAA.getSource());		
	}
	public void test31() throws Exception {
		createCUfromTestFile(getPackageP(), "AA");
		
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
				
		Refactoring ref= createRefactoring(classA, "B");
		
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		
		ICompilationUnit newcu= getPackageP().getCompilationUnit("B.java");
		ICompilationUnit newcuAA= getPackageP().getCompilationUnit("AA.java");
		assertEqualLines("invalid renaming A", getFileContents(getOutputTestFileName("B")), newcu.getSource());
		assertEqualLines("invalid renaming AA", getFileContents(getOutputTestFileName("AA")), newcuAA.getSource());		
	}
	public void test32() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test33() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test34() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test35() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test36() throws Exception { 
		helper2("A", "B");		
	}

	public void test37() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test38() throws Exception { 
		helper2("A", "B");			
	}

	public void test39() throws Exception { 
		helper2("A", "B");		
	}
		
	public void test4() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test40() throws Exception { 
		//printTestDisabledMessage("search engine bug");
		helper2("A", "B");		
	}
	
	public void test41() throws Exception { 
		helper2("A", "B");		
	}
		
	public void test42() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test43() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test44() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test45() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test46() throws Exception { 	
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		createCUfromTestFile(packageP1, "C");
		
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
				
		Refactoring ref= createRefactoring(classA, "B");
		
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		
		ICompilationUnit newcu= getPackageP().getCompilationUnit("B.java");
		ICompilationUnit newcuC= packageP1.getCompilationUnit("C.java");
		assertEqualLines("invalid renaming A", getFileContents(getOutputTestFileName("B")), newcu.getSource());
		assertEqualLines("invalid renaming C", getFileContents(getOutputTestFileName("C")), newcuC.getSource());		
	}
	
	public void test47() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test48() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test49() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test50() throws Exception { 
		printTestDisabledMessage("https://bugs.eclipse.org/bugs/show_bug.cgi?id=54948");
		if (false)
			helper2("A", "B");
	}
	
	public void test51() throws Exception { 
		IPackageFragment packageP1= getRoot().createPackageFragment("p1", true, null);
		createCUfromTestFile(packageP1, "C");
		
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
				
		Refactoring ref= createRefactoring(classA, "B");
		
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		
		ICompilationUnit newcu= getPackageP().getCompilationUnit("B.java");
		ICompilationUnit newcuC= packageP1.getCompilationUnit("C.java");
		assertEqualLines("invalid renaming A", getFileContents(getOutputTestFileName("B")), newcu.getSource());
		assertEqualLines("invalid renaming C", getFileContents(getOutputTestFileName("C")), newcuC.getSource());		
	}
	
	public void test52() throws Exception {
		//printTestDisabledMessage("1GJY2XN: ITPJUI:WIN2000 - rename type: error when with reference");
		helper2("A", "B");		
	}

	public void test53() throws Exception { 
		helper2("A", "B", false);		
	}
	
	public void test54() throws Exception { 
		//printTestDisabledMessage("waiting for: 1GKAQJS: ITPJCORE:WIN2000 - search: incorrect results for nested types");
		helperWithTextual("A", "X", "XYZ", "A", true, false);		
	}
	
	public void test55() throws Exception { 
		//printTestDisabledMessage("waiting for: 1GKAQJS: ITPJCORE:WIN2000 - search: incorrect results for nested types");
		helperWithTextual("A", "X", "XYZ", "A", false, false);		
	}
	
	public void test57() throws Exception {
		helperWithTextual("A", "A", "B", "B", true, true);
	}
	
	public void test58() throws Exception {
		//printTestDisabledMessage("bug#16751");
		helper2("A", "B");
	}

	public void test59() throws Exception {
//		printTestDisabledMessage("bug#22938");
		helper2("A", "B");
	}

	public void test60() throws Exception {
//		printTestDisabledMessage("test for bug 24740");
		helperWithTextual("A", "A", "B", "B", true, true);
	}
	
	public void test61() throws Exception {
		ParticipantTesting.reset();
		String[] renameHandles= helperWithTextual("A" , "Inner", "InnerB", "A", true, false);
		ParticipantTesting.testRename(renameHandles,
			new RenameArguments[] {
				new RenameArguments("InnerB", true), 
			});
	}
		
	public void test5() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test6() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test7() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test8() throws Exception { 
		helper2("A", "B");		
	}
	
	public void test9() throws Exception { 
		helper2("A", "B");		
	}
	
	public void testQualifiedName1() throws Exception {
		getRoot().createPackageFragment("p", true, null);
		
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
		
		String content= getFileContents(getTestPath() + "testQualifiedName1/in/build.xml");
		IProject project= classA.getJavaProject().getProject();
		IFile file= project.getFile("build.xml");
		file.create(new ByteArrayInputStream(content.getBytes()), true, null);
				
		RenameRefactoring ref= createRefactoring(classA, "B");
		
		IQualifiedNameUpdating qr= (IQualifiedNameUpdating)ref.getProcessor();
		qr.setUpdateQualifiedNames(true);
		qr.setFilePatterns("*.xml");
		
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		
		ICompilationUnit newcu= getPackageP().getCompilationUnit("B.java");
		assertEqualLines("invalid renaming A", getFileContents(getOutputTestFileName("B")), newcu.getSource());
		InputStreamReader reader= new InputStreamReader(file.getContents(true));
		StringBuffer newContent= new StringBuffer();
		int ch;
		try {
			while((ch= reader.read()) != -1)
				newContent.append((char)ch);
		} finally {
			if (reader != null)
				reader.close();
		}
		String definedContent= getFileContents(getTestPath() + "testQualifiedName1/out/build.xml");
		assertEqualLines("invalid updating build.xml", newContent.toString(), definedContent);
		
	}
}
