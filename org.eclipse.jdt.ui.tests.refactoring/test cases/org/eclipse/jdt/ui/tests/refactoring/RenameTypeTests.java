/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDerivedElementUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;

import org.eclipse.jdt.ui.tests.refactoring.infra.DebugUtils;

public class RenameTypeTests extends RefactoringTest {
	
	private static final Class clazz= RenameTypeTests.class;
	private static final String REFACTORING_PATH= "RenameType/";
	
	private static final boolean BUG_83012_core_annotation_search= true;
	
	public RenameTypeTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}
	
	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
		
	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, className), className);
	}
		
	private RenameRefactoring createRefactoring(IType type, String newName) throws CoreException {
		RenameRefactoring ref= new RenameRefactoring(new RenameTypeProcessor(type));
		((INameUpdating)ref.getAdapter(INameUpdating.class)).setNewElementName(newName);
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
		IReferenceUpdating refUpdating= (IReferenceUpdating)ref.getAdapter(IReferenceUpdating.class);
		refUpdating.setUpdateReferences(updateReferences);
		ITextUpdating textUpdating= (ITextUpdating)ref.getAdapter(ITextUpdating.class);
		textUpdating.setUpdateTextualMatches(updateTextualMatches);
		
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		ICompilationUnit newcu= pack.getCompilationUnit(newCUName + ".java");
		assertTrue("cu " + newcu.getElementName()+ " does not exist", newcu.exists());
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName(newCUName)), newcu.getSource());
		
		INameUpdating nameUpdating= ((INameUpdating)ref.getAdapter(INameUpdating.class));
		IType newElement = (IType) nameUpdating.getNewElement();
		assertTrue("new element does not exist:\n" + newElement.toString(), newElement.exists());
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
	
	// <--------------------- Derived Member ---------------------------->
	
	protected void setUp() throws Exception {
		super.setUp();
		setSomeFieldOptions(getPackageP().getJavaProject(), "f", "Suf1", false);
		setSomeFieldOptions(getPackageP().getJavaProject(), "fs", "_suffix", true);
		setSomeLocalOptions(getPackageP().getJavaProject(), "lv", "_lv");
		setSomeArgumentOptions(getPackageP().getJavaProject(), "pm", "_pm");
	}
	
	private void helper3(String oldName, String newName, boolean updateRef, boolean updateTextual, boolean updateDerived, String nonJavaFiles) throws JavaModelException, CoreException, IOException, Exception {
		RenameRefactoring ref= initWithAllOptions(oldName, oldName, newName, newName, updateRef, updateTextual, updateDerived, nonJavaFiles, RenamingNameSuggestor.STRATEGY_EMBEDDED);
		RefactoringStatus status= performRefactoring(ref);
		assertNull("was supposed to pass", status);
		checkResultInClass(newName);
	}
	
	private void helper3_inner(String oldName, String oldInnerName, String newName, String innerNewName, boolean updateRef, boolean updateTextual, boolean updateDerived, String nonJavaFiles) throws JavaModelException, CoreException, IOException, Exception {
		RenameRefactoring ref= initWithAllOptions(oldName, oldInnerName, newName, innerNewName, updateRef, updateTextual, updateDerived, nonJavaFiles, RenamingNameSuggestor.STRATEGY_EMBEDDED);
		assertNull("was supposed to pass", performRefactoring(ref));
		checkResultInClass(newName);
	}
	
	private void helper3(String oldName, String newName, boolean updateDerived, boolean updateTextual, boolean updateRef) throws JavaModelException, CoreException, IOException, Exception {
		helper3(oldName, newName, updateDerived, updateTextual, updateRef, null);
	}
	
	private void helper3_fail(String oldName, String newName, boolean updateDerived, boolean updateTextual, boolean updateRef, int matchStrategy) throws JavaModelException, CoreException, IOException, Exception {
		RenameRefactoring ref= initWithAllOptions(oldName, oldName, newName, newName, updateRef, updateTextual, updateRef, null, matchStrategy);
		assertNotNull("was supposed to fail", performRefactoring(ref));
	}
	
	private void helper3_fail(String oldName, String newName, boolean updateDerived, boolean updateTextual, boolean updateRef) throws JavaModelException, CoreException, IOException, Exception {
		RenameRefactoring ref= initWithAllOptions(oldName, oldName, newName, newName, updateRef, updateTextual, updateRef, null, RenamingNameSuggestor.STRATEGY_EMBEDDED);
		assertNotNull("was supposed to fail", performRefactoring(ref));
	}

	private RenameRefactoring initWithAllOptions(String oldName, String innerOldName, String newName, String innerNewName, boolean updateReferences, boolean updateTextualMatches, boolean updateDerived, String nonJavaFiles, int matchStrategy) throws Exception, JavaModelException, CoreException {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), oldName);
		IType classA= getType(cu, innerOldName);
		RenameRefactoring ref= createRefactoring(classA, innerNewName);
		setTheOptions(ref, updateReferences, updateTextualMatches, updateDerived, nonJavaFiles, matchStrategy);
		return ref;
	}

	private void setTheOptions(RenameRefactoring ref, boolean updateReferences, boolean updateTextualMatches, boolean updateDerived, String nonJavaFiles, int matchStrategy) {
		IReferenceUpdating refUpdating= (IReferenceUpdating)ref.getAdapter(IReferenceUpdating.class);
		refUpdating.setUpdateReferences(updateReferences);
		ITextUpdating textUpdating= (ITextUpdating)ref.getAdapter(ITextUpdating.class);
		textUpdating.setUpdateTextualMatches(updateTextualMatches);
		if (nonJavaFiles!=null) {
			IQualifiedNameUpdating qnUpdating= (IQualifiedNameUpdating)ref.getAdapter(IQualifiedNameUpdating.class);
			qnUpdating.setUpdateQualifiedNames(true);
			qnUpdating.setFilePatterns(nonJavaFiles);
		}
		
		IDerivedElementUpdating p= (IDerivedElementUpdating)ref.getAdapter(IDerivedElementUpdating.class);
		p.setUpdateDerivedElements(updateDerived);
		p.setMatchStrategy(matchStrategy);
	}
	
	private void checkResultInClass(String typeName) throws JavaModelException, IOException {
		ICompilationUnit newcu= getPackageP().getCompilationUnit(typeName + ".java");
		assertTrue("cu " + newcu.getElementName()+ " does not exist", newcu.exists());
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName(typeName)), newcu.getSource());
	}
	
	private void setSomeFieldOptions(IJavaProject project, String prefixes, String suffixes, boolean forStatic) {
		if (forStatic) {
			project.setOption(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, prefixes);
			project.setOption(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, suffixes);
		}
		else {
			project.setOption(JavaCore.CODEASSIST_FIELD_PREFIXES, prefixes);
			project.setOption(JavaCore.CODEASSIST_FIELD_SUFFIXES, suffixes);
		}
	}
	
	private void setSomeLocalOptions(IJavaProject project, String prefixes, String suffixes) {
			project.setOption(JavaCore.CODEASSIST_LOCAL_PREFIXES, prefixes);
			project.setOption(JavaCore.CODEASSIST_LOCAL_SUFFIXES, suffixes);
	}
	
	private void setSomeArgumentOptions(IJavaProject project, String prefixes, String suffixes) {
		project.setOption(JavaCore.CODEASSIST_ARGUMENT_PREFIXES, prefixes);
		project.setOption(JavaCore.CODEASSIST_ARGUMENT_SUFFIXES, suffixes);
	}
	
	// </------------------------------------ Derived Member --------------------------------->
	
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
		if (Platform.getOS().equals(Platform.OS_WIN32))
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
	
	public void test62() throws Exception {
//		printTestDisabledMessage("test for bug 66250");
		helperWithTextual("A", "A", "B", "B", false, true);
	}
	
	public void test63() throws Exception {
//		printTestDisabledMessage("test for bug 79131");
		IPackageFragment anotherP= getRoot().createPackageFragment("another.p", true, null);
		String folder= "anotherp/";
		String type= "A";
		ICompilationUnit cu= createCUfromTestFile(anotherP, type, folder);
		
		helperWithTextual(type, type, "B", "B", true, true);

		assertEqualLines("invalid renaming in another.p.A", getFileContents(getOutputTestFileName(type, folder)), cu.getSource());
	}
	
	public void test64() throws Exception {
//		printTestDisabledMessage("test for bug 79131");
		IPackageFragment p2= getRoot().createPackageFragment("p2", true, null);
		String folder= "p2/";
		String type= "A";
		ICompilationUnit cu= createCUfromTestFile(p2, type, folder);
		
		helperWithTextual(type, type, "B", "B", true, true);

		assertEqualLines("invalid renaming in p2.A", getFileContents(getOutputTestFileName(type, folder)), cu.getSource());
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
		
		IQualifiedNameUpdating qr= (IQualifiedNameUpdating)ref.getAdapter(IQualifiedNameUpdating.class);
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
	
	public void testGenerics1() throws Exception {
		helper2("A", "B");
	}
	
	public void testGenerics2() throws Exception {
		helper2("A", "B");
	}
	
	public void testGenerics3() throws Exception {
		helper2("A", "B");
	}
	
	public void testGenerics4() throws Exception {
		helper2("A", "B");
	}
	
	public void testEnum1() throws Exception {		
		IPackageFragment p2= getRoot().createPackageFragment("p2", true, null);
		String folder= "p2/";
		String type= "A";
		ICompilationUnit cu= createCUfromTestFile(p2, type, folder);
		
		helper2("A", "B");

		assertEqualLines("invalid renaming in p2.A", getFileContents(getOutputTestFileName(type, folder)), cu.getSource());
	}

	public void testEnum2() throws Exception {
		helper2("A", "B");
	}
	
	public void testEnum3() throws Exception {
		ICompilationUnit enumbered= createCUfromTestFile(getPackageP(), "Enumbered");
		helper2("A", "B");
		assertEqualLines("invalid renaming in Enumbered", getFileContents(getOutputTestFileName("Enumbered")), enumbered.getSource());
	}
	
	public void testEnum4() throws Exception {
		helperWithTextual("A" , "A", "B", "A", true, false);
	}
	
	public void testEnum5() throws Exception {
		helper2("A", "B");
	}
	
	public void testAnnotation1() throws Exception {
		helper2("A", "B");
	}
	
	public void testAnnotation2() throws Exception {
		if (BUG_83012_core_annotation_search)
			return;
		
		helper2("A", "B");
	}
	
	public void testAnnotation3() throws Exception {
		helperWithTextual("A" , "A", "B", "A", true, true);
	}
	
	// --------------- Derived  tests -----------------
	
	public void testDerivedElements00() throws Exception {
		// Very basic test, one field, two methods
		helper3("SomeClass", "SomeClass2", true, false, true);
	}
	
	public void testDerivedElements01() throws Exception {
		// Already existing field with new name, shadow-error from field refac
		helper3_fail("SomeClass", "SomeClass2", true, false, true);
	}
	
	public void testDerivedElements02() throws Exception {
		// Already existing method like new setter, shadow-error from field refac
		helper3_fail("SomeClass", "SomeClass2", true, false, true);
	}
	
	public void testDerivedElements03() throws Exception {
		// more methods
		helper3("SomeClass", "SomeClass2", true, false, true);
	}
	public void testDerivedElements04() throws Exception {
		//Additional field with exactly the same name and getters and setters in another class
		getClassFromTestFile(getPackageP(), "SomeOtherClass");
		helper3("SomeClass", "SomeClass2", true, false, true);
		checkResultInClass("SomeOtherClass");
	}
	
	public void testDerivedElements05() throws Exception {
		//rename textual and qualified
		String content= getFileContents(getTestPath() + "testDerivedElements05/in/test.html");
		IProject project= getPackageP().getJavaProject().getProject();
		IFile file= project.getFile("test.html");
		file.create(new ByteArrayInputStream(content.getBytes()), true, null);
		
		helper3("SomeClass", "SomeDifferentClass", true, true, true, "test.html");
		
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
		String definedContent= getFileContents(getTestPath() + "testDerivedElements05/out/test.html");
		assertEqualLines("invalid updating test.html", newContent.toString(), definedContent);
		
	}
	
	public void testDerivedElements06() throws Exception {
		//Additional field with exactly the same name and getters and setters in another class
		//incl. textual
		// printTestDisabledMessage("potential matches in comments issue (bug 111891)");
		getClassFromTestFile(getPackageP(), "SomeNearlyIdenticalClass");
		helper3("SomeClass", "SomeOtherClass", true, true, true);
		checkResultInClass("SomeNearlyIdenticalClass");
	}
	
	public void testDerivedElements07() throws Exception {
		//Test 4 fields in one file, different suffixes/prefixes, incl. 2x setters/getters
		helper3("SomeClass", "SomeDiffClass", true, true, true);
	}
	
	public void testDerivedElements08() throws Exception {
		//Interface renaming fun, this time without textual
		helper3("ISomeIf", "ISomeIf2", true, false, true);
	}

	public void testDerivedElements09() throws Exception {
		//Some inner types
		getClassFromTestFile(getPackageP(), "SomeOtherClass");
		helper3_inner("SomeClass", "SomeInnerClass", "SomeClass", "SomeNewInnerClass", true, true, true, null);
		checkResultInClass("SomeOtherClass");
	}
	
	public void testDerivedElements10() throws Exception {
		//Two static fields
		getClassFromTestFile(getPackageP(), "SomeOtherClass");
		helper3("SomeClass", "SomeClass2", true, true, true, null);
		checkResultInClass("SomeOtherClass");
	}
	
	public void testDerivedElements11() throws Exception {
		//Assure participants get notified of normal stuff (type rename
		//and resource changes) AND derived elements. 
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "SomeClass");
		IType someClass= getType(cu, "SomeClass");
		IType other= getClassFromTestFile(getPackageP(), "SomeOtherClass");
		
		List handleList= new ArrayList();
		List argumentList= new ArrayList();
		
		List derivedOldHandleList= new ArrayList();
		List derivedNewNameList= new ArrayList();
		List derivedNewHandleList= new ArrayList();
		
		final String newName= "SomeNewClass";
		
		// f-Field + getters/setters
		IField f3= other.getField("fSomeClass");
		derivedOldHandleList.add(f3.getHandleIdentifier());
		derivedNewHandleList.add("Lp/SomeOtherClass;.fSomeNewClass");
		derivedNewNameList.add("fSomeNewClass");
		
		IMethod m3= other.getMethod("getSomeClass", new String[0]);
		derivedOldHandleList.add(m3.getHandleIdentifier());
		derivedNewNameList.add("getSomeNewClass");
		derivedNewHandleList.add("Lp/SomeOtherClass;.getSomeNewClass()V");
		IMethod m4= other.getMethod("setSomeClass", new String[] {"QSomeClass;"});
		derivedOldHandleList.add(m4.getHandleIdentifier());
		derivedNewNameList.add("setSomeNewClass");
		derivedNewHandleList.add("Lp/SomeOtherClass;.setSomeNewClass(QSomeNewClass;)V");
		
		// non-f-field + getter/setters
		IField f1= someClass.getField("someClass");
		derivedOldHandleList.add(f1.getHandleIdentifier());
		derivedNewNameList.add("someNewClass");
		derivedNewHandleList.add("Lp/SomeNewClass;.someNewClass");
		IMethod m1= someClass.getMethod("getSomeClass", new String[0]);
		derivedOldHandleList.add(m1.getHandleIdentifier());
		derivedNewNameList.add("getSomeNewClass");
		derivedNewHandleList.add("Lp/SomeNewClass;.getSomeNewClass()V");
		IMethod m2= someClass.getMethod("setSomeClass", new String[] {"QSomeClass;"});
		derivedOldHandleList.add(m2.getHandleIdentifier());
		derivedNewNameList.add("setSomeNewClass");
		derivedNewHandleList.add("Lp/SomeNewClass;.setSomeNewClass(QSomeNewClass;)V");

		// fs-field
		IField f2= someClass.getField("fsSomeClass");
		derivedOldHandleList.add(f2.getHandleIdentifier());
		derivedNewNameList.add("fsSomeNewClass");
		derivedNewHandleList.add("Lp/SomeNewClass;.fsSomeNewClass");
		
		// Type Stuff
		handleList.add(someClass);
		argumentList.add(new RenameArguments(newName, true));
		handleList.add(cu);
		argumentList.add(new RenameArguments(newName + ".java", true));
		handleList.add(cu.getResource());
		argumentList.add(new RenameArguments(newName + ".java", true));
		
		String[] handles= ParticipantTesting.createHandles(handleList.toArray());
		RenameArguments[] arguments= (RenameArguments[])argumentList.toArray(new RenameArguments[0]);
		
		RenameRefactoring ref= createRefactoring(someClass, newName);
		setTheOptions(ref, true, true, true, null, RenamingNameSuggestor.STRATEGY_EMBEDDED);
		RefactoringStatus status= performRefactoring(ref);
		assertNull("was supposed to pass", status);
		
		checkResultInClass(newName);
		checkResultInClass("SomeOtherClass");
		
		ParticipantTesting.testRename(handles, arguments);
		ParticipantTesting.testDerivedElements(derivedOldHandleList, derivedNewNameList, derivedNewHandleList);
	}
	
	public void testDerivedElements12() throws Exception {
		// Test updating of references
		helper3("SomeFieldClass", "SomeOtherFieldClass", true, true, true);
	}
	
	public void testDerivedElements13() throws Exception {
		// Test various locals and parameters with and without prefixes.
		// tests not renaming parameters with local prefixes and locals with parameter prefixes
		printTestDisabledMessage("Local variables not currently supported.");
		//helper3("SomeClass", "SomeOtherClass", true, false, true, true);
	}
	
	public void testDerivedElements14() throws Exception {
		// Test for loop variables
		printTestDisabledMessage("Local variables not currently supported.");
		//helper3("SomeClass2", "SomeOtherClass2", true, false, true, true);
	}
	
	public void testDerivedElements15() throws Exception {
		// Test catch block variables (exceptions)
		printTestDisabledMessage("Local variables not currently supported.");
		//helper3("SomeClass3", "SomeOtherClass3", true, false, true, true);
	}
	
	public void testDerivedElements16() throws Exception {
		// Test updating of references
		printTestDisabledMessage("Local variables not currently supported.");
		//helper3("SomeClass4", "SomeOtherClass4", true, false, true, true);
	}
	
	public void testDerivedElements17() throws Exception {
		// Local with this name already exists - do not pass.
		printTestDisabledMessage("Local variables not currently supported.");
		//helper3_fail("SomeClass6", "SomeOtherClass6", true, false, true, true);
	}
	
	public void testDerivedElements18() throws Exception {
		// factory method
		helper3("SomeClass", "SomeOtherClass", true, false, true);
	}
	
	public void testDerivedElements19() throws Exception {
		// Test detection of same target
		helper3_fail("ThreeHunkClass", "TwoHunk", true, false, true, RenamingNameSuggestor.STRATEGY_SUFFIX);
	}
	
	public void testDerivedElements20() throws Exception {
		// Overridden method, check both are renamed
		getClassFromTestFile(getPackageP(), "OtherClass");
		helper3("OverriddenMethodClass", "ThirdClass", true, true, true);
		checkResultInClass("OtherClass");
	}
	
	public void testDerivedElements21() throws Exception {
		// Constructors may not be renamed
		getClassFromTestFile(getPackageP(), "SomeClassSecond");
		helper3("SomeClass", "SomeNewClass", true, false, true);
		checkResultInClass("SomeClassSecond");
	}
	
	public void testDerivedElements22() throws Exception {
		// Test transplanter for fields in types inside of initializers

		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "SomeClass");
		IType someClass= getType(cu, "SomeClass");
		
		List handleList= new ArrayList();
		List argumentList= new ArrayList();
		
		List derivedOldHandleList= new ArrayList();
		List derivedNewNameList= new ArrayList();
		List derivedNewHandleList= new ArrayList();
		
		final String newName= "SomeNewClass";
		
		// field in class in initializer
		IField inInitializer= someClass.getInitializer(1).getType("InInitializer", 1).getField("someClassInInitializer");
		derivedOldHandleList.add(inInitializer.getHandleIdentifier());
		derivedNewNameList.add("someNewClassInInitializer");
		derivedNewHandleList.add("Lp/SomeNewClass$InInitializer;.someNewClassInInitializer");
		
		// Type Stuff
		handleList.add(someClass);
		argumentList.add(new RenameArguments(newName, true));
		handleList.add(cu);
		argumentList.add(new RenameArguments(newName + ".java", true));
		handleList.add(cu.getResource());
		argumentList.add(new RenameArguments(newName + ".java", true));
		
		String[] handles= ParticipantTesting.createHandles(handleList.toArray());
		RenameArguments[] arguments= (RenameArguments[])argumentList.toArray(new RenameArguments[0]);
		
		RenameRefactoring ref= createRefactoring(someClass, newName);
		setTheOptions(ref, true, true, true, null, RenamingNameSuggestor.STRATEGY_EMBEDDED);
		RefactoringStatus status= performRefactoring(ref);
		assertNull("was supposed to pass", status);
		
		checkResultInClass(newName);
		
		ParticipantTesting.testRename(handles, arguments);
		ParticipantTesting.testDerivedElements(derivedOldHandleList, derivedNewNameList, derivedNewHandleList);
		
	}
	
	public void testDerivedElements23() throws Exception {
		// Test transplanter for elements inside types inside fields

		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "SomeClass");
		IType someClass= getType(cu, "SomeClass");
		
		List handleList= new ArrayList();
		List argumentList= new ArrayList();
		
		List derivedOldHandleList= new ArrayList();
		List derivedNewNameList= new ArrayList();
		List derivedNewHandleList= new ArrayList();
		
		final String newName= "SomeNewClass";
		
		// some field 
		IField anotherSomeClass= someClass.getField("anotherSomeClass");
		derivedOldHandleList.add(anotherSomeClass.getHandleIdentifier());
		derivedNewNameList.add("anotherSomeNewClass");
		derivedNewHandleList.add("Lp/SomeNewClass;.anotherSomeNewClass");
		
		// field in class in method in field declaration ;)
		IField inInner= anotherSomeClass.getType("", 1).getMethod("foo", new String[0]).getType("X", 1).getField("someClassInInner");
		derivedOldHandleList.add(inInner.getHandleIdentifier());
		derivedNewNameList.add("someNewClassInInner");
		derivedNewHandleList.add("Lp/SomeNewClass$1$X;.someNewClassInInner");
		
		// Type Stuff
		handleList.add(someClass);
		argumentList.add(new RenameArguments(newName, true));
		handleList.add(cu);
		argumentList.add(new RenameArguments(newName + ".java", true));
		handleList.add(cu.getResource());
		argumentList.add(new RenameArguments(newName + ".java", true));
		
		String[] handles= ParticipantTesting.createHandles(handleList.toArray());
		RenameArguments[] arguments= (RenameArguments[])argumentList.toArray(new RenameArguments[0]);
		
		RenameRefactoring ref= createRefactoring(someClass, newName);
		setTheOptions(ref, true, true, true, null, RenamingNameSuggestor.STRATEGY_EMBEDDED);
		RefactoringStatus status= performRefactoring(ref);
		assertNull("was supposed to pass", status);
		
		checkResultInClass(newName);
		
		ParticipantTesting.testRename(handles, arguments);
		ParticipantTesting.testDerivedElements(derivedOldHandleList, derivedNewNameList, derivedNewHandleList);
	}
	
	public void testDerivedElements24() throws Exception {
		// Test transplanter for ICompilationUnit and IFile
		
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "SomeClass");
		IType someClass= getType(cu, "SomeClass");
		
		RenameRefactoring ref= createRefactoring(someClass, "SomeNewClass");
		setTheOptions(ref, true, true, true, null, RenamingNameSuggestor.STRATEGY_EMBEDDED);
		RefactoringStatus status= performRefactoring(ref);
		assertNull("was supposed to pass", status);
		
		checkResultInClass("SomeNewClass");
		
		RenameTypeProcessor rtp= (RenameTypeProcessor)ref.getProcessor();
		ICompilationUnit newUnit= (ICompilationUnit) rtp.getRefactoredElement(someClass.getCompilationUnit());
		
		assertTrue(newUnit.exists());
		assertTrue(newUnit.getElementName().equals("SomeNewClass.java"));
		assertFalse(someClass.getCompilationUnit().exists());
		
		IFile newFile= (IFile) rtp.getRefactoredElement(someClass.getResource());
		
		assertTrue(newFile.exists());
		assertTrue(newFile.getName().equals("SomeNewClass.java"));
		assertFalse(someClass.getResource().exists());
		
		IPackageFragment oldPackage= (IPackageFragment) cu.getParent();
		IPackageFragment newPackage= (IPackageFragment) rtp.getRefactoredElement(oldPackage);
		assertEquals(oldPackage, newPackage);
		
	}
	
}
