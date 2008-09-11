/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla - 228950: [pull up] exception if target calls super with multiple parameters
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class PullUpTests extends RefactoringTest {

	private static final boolean BUG_91542= true;

	private static final Class clazz= PullUpTests.class;

	private static final String REFACTORING_PATH= "PullUp/";

	public PullUpTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java15Setup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java15Setup(someTest);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//-------------------

	private static PullUpRefactoringProcessor createRefactoringProcessor(IMember[] methods) throws JavaModelException{
		IJavaProject project= null;
		if (methods != null && methods.length > 0)
			project= methods[0].getJavaProject();
		if (RefactoringAvailabilityTester.isPullUpAvailable(methods)) {
			PullUpRefactoringProcessor processor= new PullUpRefactoringProcessor(methods, JavaPreferencesSettings.getCodeGenerationSettings(project));
			new ProcessorBasedRefactoring(processor);
			return processor;
		}
		return null;
	}

	private void fieldMethodHelper1(String[] fieldNames, String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IField[] fields= getFields(type, fieldNames);
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(merge(methods, fields));

			Refactoring ref= processor.getRefactoring();
			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setSuperclassAsTargetClass(processor);

			if (deleteAllInSourceType)
				processor.setDeletedMethods(methods);
			if (deleteAllMatchingMethods)
				processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
			assertTrue("precondition was supposed to pass", !checkInputResult.hasError());
			performChange(ref, false);

			String expected= getFileContents(getOutputTestFileName("A"));
			String actual= cu.getSource();
			assertEqualLines(expected, actual);
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}
	}

	private IType[] getPossibleTargetClasses(PullUpRefactoringProcessor processor) throws JavaModelException {
		return processor.getCandidateTypes(new RefactoringStatus(), new NullProgressMonitor());
	}

	private void setSuperclassAsTargetClass(PullUpRefactoringProcessor processor) throws JavaModelException {
		IType[] possibleClasses= getPossibleTargetClasses(processor);
		processor.setDestinationType(possibleClasses[possibleClasses.length - 1]);
	}

	private void setTargetClass(PullUpRefactoringProcessor processor, int targetClassIndex) throws JavaModelException {
		IType[] possibleClasses= getPossibleTargetClasses(processor);
		processor.setDestinationType(getPossibleTargetClasses(processor)[possibleClasses.length - 1 - targetClassIndex]);
	}

	private void addRequiredMembersHelper(String[] fieldNames, String[] methodNames, String[][] methodSignatures, String[] expectedFieldNames, String[] expectedMethodNames, String[][] expectedMethodSignatures) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IField[] fields= getFields(type, fieldNames);
			IMethod[] methods= getMethods(type, methodNames, methodSignatures);

			IMember[] members= merge(methods, fields);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(members);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setSuperclassAsTargetClass(processor);

			List additionalRequired= Arrays.asList(processor.getAdditionalRequiredMembersToPullUp(new NullProgressMonitor()));
			List required= new ArrayList();
			required.addAll(additionalRequired);
			required.addAll(Arrays.asList(members));
			IField[] expectedFields= getFields(type, expectedFieldNames);
			IMethod[] expectedMethods= getMethods(type, expectedMethodNames, expectedMethodSignatures);
			List expected= Arrays.asList(merge(expectedFields, expectedMethods));
			assertEquals("incorrect size", expected.size(), required.size());
			for (Iterator iter= expected.iterator(); iter.hasNext();) {
				Object each= iter.next();
				assertTrue ("required does not contain " + each, required.contains(each));
			}
			for (Iterator iter= required.iterator(); iter.hasNext();) {
				Object each= iter.next();
				assertTrue ("expected does not contain " + each, expected.contains(each));
			}
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}
	}

	private void fieldHelper1(String[] fieldNames, int targetClassIndex) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IField[] fields= getFields(type, fieldNames);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(fields);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setTargetClass(processor, targetClassIndex);

			RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
			assertTrue("precondition was supposed to pass", !checkInputResult.hasError());
			performChange(ref, false);

			String expected= getFileContents(getOutputTestFileName("A"));
			String actual= cu.getSource();
			assertEqualLines(expected, actual);
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}
	}

	private void fieldHelper2(String[] fieldNames, int targetClassIndex) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IField[] fields= getFields(type, fieldNames);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(fields);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setTargetClass(processor, targetClassIndex);

			RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
			assertTrue("precondition was supposed to fail", !checkInputResult.isOK());
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}
	}

	private static IMethod[] getMethods(IMember[] members){
		List l= Arrays.asList(JavaElementUtil.getElementsOfType(members, IJavaElement.METHOD));
		return (IMethod[]) l.toArray(new IMethod[l.size()]);
	}

	private Refactoring createRefactoringPrepareForInputCheck(String[] selectedMethodNames, String[][] selectedMethodSignatures,
						String[] selectedFieldNames,
						String[] selectedTypeNames, String[] namesOfMethodsToPullUp,
						String[][] signaturesOfMethodsToPullUp,
						String[] namesOfFieldsToPullUp, String[] namesOfTypesToPullUp,
						String[] namesOfMethodsToDeclareAbstract, String[][] signaturesOfMethodsToDeclareAbstract,
						boolean deleteAllPulledUpMethods, boolean deleteAllMatchingMethods, int targetClassIndex, ICompilationUnit cu) throws CoreException {
		IType type= getType(cu, "B");
		IMethod[] selectedMethods= getMethods(type, selectedMethodNames, selectedMethodSignatures);
		IField[] selectedFields= getFields(type, selectedFieldNames);
		IType[] selectedTypes= getMemberTypes(type, selectedTypeNames);
		IMember[] selectedMembers= merge(selectedFields, selectedMethods, selectedTypes);

		PullUpRefactoringProcessor processor= createRefactoringProcessor(selectedMembers);
		Refactoring ref= processor.getRefactoring();

		assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());

		setTargetClass(processor, targetClassIndex);

		IMethod[] methodsToPullUp= findMethods(selectedMethods, namesOfMethodsToPullUp, signaturesOfMethodsToPullUp);
		IField[] fieldsToPullUp= findFields(selectedFields, namesOfFieldsToPullUp);
		IType[] typesToPullUp= findTypes(selectedTypes, namesOfTypesToPullUp);
		IMember[] membersToPullUp= merge(methodsToPullUp, fieldsToPullUp, typesToPullUp);

		IMethod[] methodsToDeclareAbstract= findMethods(selectedMethods, namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract);

		processor.setMembersToMove(membersToPullUp);
		processor.setAbstractMethods(methodsToDeclareAbstract);
		if (deleteAllPulledUpMethods && methodsToPullUp.length != 0)
			processor.setDeletedMethods(methodsToPullUp);
		if (deleteAllMatchingMethods && methodsToPullUp.length != 0)
			processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));
		return ref;
	}

	private void declareAbstractFailHelper(String[] selectedMethodNames, String[][] selectedMethodSignatures,
											String[] selectedFieldNames,
											String[] selectedTypeNames, String[] namesOfMethodsToPullUp,
											String[][] signaturesOfMethodsToPullUp, String[] namesOfFieldsToPullUp,
											String[] namesOfMethodsToDeclareAbstract,
											String[][] signaturesOfMethodsToDeclareAbstract, String[] namesOfTypesToPullUp,
											boolean deleteAllPulledUpMethods, boolean deleteAllMatchingMethods, int targetClassIndex) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			Refactoring ref= createRefactoringPrepareForInputCheck(selectedMethodNames, selectedMethodSignatures, selectedFieldNames, selectedTypeNames, namesOfMethodsToPullUp,
					signaturesOfMethodsToPullUp, namesOfFieldsToPullUp, namesOfTypesToPullUp, namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, deleteAllPulledUpMethods,
					deleteAllMatchingMethods, targetClassIndex, cu);

			RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
			assertTrue("precondition was supposed to fail", !checkInputResult.isOK());
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}
	}

	private void declareAbstractHelper(String[] selectedMethodNames, String[][] selectedMethodSignatures,
											String[] selectedFieldNames,
											String[] selectedTypeNames, String[] namesOfMethodsToPullUp,
											String[][] signaturesOfMethodsToPullUp, String[] namesOfFieldsToPullUp,
											String[] namesOfMethodsToDeclareAbstract,
											String[][] signaturesOfMethodsToDeclareAbstract, String[] namesOfTypesToPullUp,
											boolean deleteAllPulledUpMethods, boolean deleteAllMatchingMethods, int targetClassIndex) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			Refactoring ref= createRefactoringPrepareForInputCheck(selectedMethodNames, selectedMethodSignatures, selectedFieldNames, selectedTypeNames, namesOfMethodsToPullUp,
					signaturesOfMethodsToPullUp, namesOfFieldsToPullUp, namesOfTypesToPullUp, namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, deleteAllPulledUpMethods,
					deleteAllMatchingMethods, targetClassIndex, cu);

			RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
			assertTrue("precondition was supposed to pass", !checkInputResult.hasError());
			performChange(ref, false);

			String expected= getFileContents(getOutputTestFileName("A"));
			String actual= cu.getSource();
			assertEqualLines(expected, actual);
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}
	}

	private void helper1(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods, int targetClassIndex) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());

			setTargetClass(processor, targetClassIndex);

			if (deleteAllInSourceType)
				processor.setDeletedMethods(methods);
			if (deleteAllMatchingMethods)
				processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
			assertTrue("precondition was supposed to pass", !checkInputResult.hasError());
			performChange(ref, false);

			String expected= getFileContents(getOutputTestFileName("A"));
			String actual= cu.getSource();
			assertEqualLines(expected, actual);
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}
	}

	private void helper2(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods, int targetClassIndex) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setTargetClass(processor, targetClassIndex);

			if (deleteAllInSourceType)
				processor.setDeletedMethods(methods);
			if (deleteAllMatchingMethods)
				processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
			assertTrue("precondition was supposed to fail", !checkInputResult.isOK());
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}
	}

	private void helper3(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods, int targetClassIndex, boolean shouldActivationCheckPass) throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
		try{
			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertEquals("activation", shouldActivationCheckPass, ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			if (! shouldActivationCheckPass)
				return;
			setTargetClass(processor, targetClassIndex);

			if (deleteAllInSourceType)
				processor.setDeletedMethods(methods);
			if (deleteAllMatchingMethods)
				processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
			assertTrue("precondition was supposed to fail", !checkInputResult.isOK());
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}
	}

	//------------------ tests -------------

	public void test0() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test1() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test2() throws Exception{
		helper1(new String[]{"mmm", "n"}, new String[][]{new String[0], new String[0]}, true, false, 0);
	}

	public void test3() throws Exception{
		helper1(new String[]{"mmm", "n"}, new String[][]{new String[0], new String[0]}, true, true, 0);
	}

	public void test4() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[]{"QList;"}};

			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setSuperclassAsTargetClass(processor);

			processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to pass", result == null || !result.hasError());

			assertEqualLines("A", cuA.getSource(), getFileContents(getOutputTestFileName("A")));
			assertEqualLines("B", cuB.getSource(), getFileContents(getOutputTestFileName("B")));
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}
	}

	public void test5() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[0]};

			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setSuperclassAsTargetClass(processor);

			processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to pass", result == null || !result.hasError());

			assertEqualLines("A", cuA.getSource(), getFileContents(getOutputTestFileName("A")));
			assertEqualLines("B", cuB.getSource(), getFileContents(getOutputTestFileName("B")));
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}
	}

	public void test6() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test7() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test8() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test9() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test10() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test11() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test12() throws Exception{
		//printTestDisabledMessage("bug#6779 searchDeclarationsOfReferencedTyped - missing exception  types");
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[0]};

			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setSuperclassAsTargetClass(processor);

			processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to pass", result == null || !result.hasError());

			assertEqualLines("A", cuA.getSource(), getFileContents(getOutputTestFileName("A")));
			assertEqualLines("B", cuB.getSource(), getFileContents(getOutputTestFileName("B")));
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}
	}

	public void test13() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test14() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test15() throws Exception{
		printTestDisabledMessage("must fix - incorrect error");
//		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void test16() throws Exception{
		printTestDisabledMessage("must fix - incorrect error");
//		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void test17() throws Exception{
		printTestDisabledMessage("must fix - incorrect error with static method access");
//		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void test18() throws Exception{
		printTestDisabledMessage("must fix - incorrect error with static field access");
//		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void test19() throws Exception{
//		printTestDisabledMessage("bug 18438");
//		printTestDisabledMessage("bug 23324 ");
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test20() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 1);
	}

	public void test21() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 1);
	}

	public void test22() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test23() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test24() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test25() throws Exception{
//		printTestDisabledMessage("bug in ASTRewrite - extra dimensions 29553");
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void test26() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test27() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test28() throws Exception{
//		printTestDisabledMessage("unimplemented (increase method visibility if declare abstract in superclass)");
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test29() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[]{"[I"}};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test30() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[]{"[I"}};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test31() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[]{"[I"}};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test32() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test33() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= selectedMethodNames;
		String[][] signaturesOfMethodsToPullUp= selectedMethodSignatures;
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {new String[0]};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test34() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test35() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test36() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test37() throws Exception{
		String[] selectedMethodNames= {"m", "f"};
		String[][] selectedMethodSignatures= {new String[0], new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {"m"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {"f"};
		String[][] signaturesOfMethodsToDeclareAbstract= {new String[0]};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test38() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {"A"};
		String[] namesOfMethodsToPullUp= {"m"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {"A"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void test39() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {"A"};
		String[] selectedTypeNames= {"X", "Y"};
		String[] namesOfMethodsToPullUp= {"m"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {"A"};
		String[] namesOfTypesToPullUp= {"X", "Y"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void test40() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] selectedTypeNames= {};
		String[] namesOfMethodsToPullUp= {"m"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfTypesToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void test41() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"i"};
		String[] selectedTypeNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {"i"};
		String[] namesOfTypesToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void test42() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"i", "j"};
		String[] selectedTypeNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {"i", "j"};
		String[] namesOfTypesToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void test43() throws Exception{
//		printTestDisabledMessage("bug 35562 Method pull up wrongly indents javadoc comment [refactoring]");

		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] selectedTypeNames= {};
		String[] namesOfMethodsToPullUp= selectedMethodNames;
		String[][] signaturesOfMethodsToPullUp= selectedMethodSignatures;
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfTypesToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void test44() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {"A"};
		String[] selectedTypeNames= {"X", "Y"};
		String[] namesOfMethodsToPullUp= {"m"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {"A"};
		String[] namesOfTypesToPullUp= {"X", "Y"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void test45() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {"A"};
		String[] selectedTypeNames= {"X", "Y"};
		String[] namesOfMethodsToPullUp= {"m"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {"A"};
		String[] namesOfTypesToPullUp= {"X", "Y"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void test46() throws Exception{
		// for bug 196635

		String[] selectedMethodNames= {"getConst"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {"CONST"};
		String[] selectedTypeNames= {};
		String[] namesOfMethodsToPullUp= {"getConst"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {"CONST"};
		String[] namesOfTypesToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void test47() throws Exception{
		// for bug 211491

		String[] selectedMethodNames= {"method"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] selectedTypeNames= {};
		String[] namesOfMethodsToPullUp= {"method"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfTypesToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void test48() throws Exception{
		// for bug 211491, but with a super class

		String[] selectedMethodNames= {"method"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] selectedTypeNames= {};
		String[] namesOfMethodsToPullUp= {"method"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfTypesToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, false, false, 0);
	}

	public void test49() throws Exception{
		// for bug 228950

		String[] selectedMethodNames= {"g"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] selectedTypeNames= {};
		String[] namesOfMethodsToPullUp= {"g"};
		String[][] signaturesOfMethodsToPullUp= {new String[0]};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfTypesToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, false, 0);
	}

	public void testFail0() throws Exception{
//		printTestDisabledMessage("6538: searchDeclarationsOf* incorrect");
		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testFail1() throws Exception{
		printTestDisabledMessage("overloading - current limitation");
//		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void testFail2() throws Exception{
//		printTestDisabledMessage("6538: searchDeclarationsOf* incorrect");
		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testFail3() throws Exception{
//		printTestDisabledMessage("6538: searchDeclarationsOf* incorrect");
		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testFail4() throws Exception{
//		printTestDisabledMessage("6538: searchDeclarationsOf* incorrect");
		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testFail6() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;

		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 0, true);
	}

	public void testFail7() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 0, false);
	}

	public void testFail8() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 0, true);
	}

	public void testFail9() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 0, true);
	}

	public void testFail10() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 0, false);
	}

	public void testFail11() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 0, true);
	}

	public void testFail12() throws Exception{
		printTestDisabledMessage("overloading - current limitation");
//		String[] methodNames= new String[]{"m"};
//		String[][] signatures= new String[][]{new String[0]};
//		boolean deleteAllInSourceType= true;
//		boolean deleteAllMatchingMethods= false;
//		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods);
	}

	public void testFail13() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper2(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 0);
	}

	public void testFail14() throws Exception{
		//removed - this (pulling up classes) is allowed now
	}

	public void testFail15() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper2(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 1);
	}

	public void testFail16() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper2(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 1);
	}

	public void testFail17() throws Exception{
		printTestDisabledMessage("unimplemented test - see bug 29522");
//		String[] methodNames= new String[]{"m"};
//		String[][] signatures= new String[][]{new String[0]};
//		boolean deleteAllInSourceType= true;
//		boolean deleteAllMatchingMethods= false;
//		helper2(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 1);
	}

	public void testFail18() throws Exception{
		printTestDisabledMessage("unimplemented test - see bug 29522");
//		String[] methodNames= new String[]{"m"};
//		String[][] signatures= new String[][]{new String[0]};
//		boolean deleteAllInSourceType= true;
//		boolean deleteAllMatchingMethods= false;
//		helper2(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 0);
	}

	public void testFail19() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper2(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods, 1);
	}

	public void testFail20() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractFailHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void testFail21() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractFailHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void testFail22() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractFailHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void testFail23() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractFailHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void testFail24() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractFailHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								new String[0], namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 0);
	}

	public void testFail25() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {};
		String[] selectedTypeNames= {"Test"};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfTypesToPullUp= {"Test"};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractFailHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void testFail26() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {};
		String[] selectedTypeNames= {"Test"};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfTypesToPullUp= {"Test"};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractFailHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void testFail27() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {};
		String[] selectedTypeNames= {"A"};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfTypesToPullUp= {"A"};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractFailHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void testFail28() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {};
		String[] selectedTypeNames= {"Test"};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfTypesToPullUp= {"Test"};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractFailHelper(selectedMethodNames, selectedMethodSignatures,
								selectedFieldNames,
								selectedTypeNames, namesOfMethodsToPullUp,
								signaturesOfMethodsToPullUp,
								namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
								signaturesOfMethodsToDeclareAbstract, namesOfTypesToPullUp, true, true, 0);
	}

	public void testFail29() throws Exception {
		helper2(new String[] {"stop"}, new String[][]{new String[0]}, true, false, 0);
	}

	//----------------------------------------------------------
	public void testField0() throws Exception{
		fieldHelper1(new String[]{"i"}, 0);
	}

	public void testFieldFail0() throws Exception{
		fieldHelper2(new String[]{"x"}, 0);
	}

	public void testFieldFail1() throws Exception{
		fieldHelper2(new String[]{"x"}, 0);
	}

	public void testFieldFail2() throws Exception{
		fieldHelper2(new String[]{"f"}, 1);
	}

	//---------------------------------------------------------
	public void testFieldMethod0() throws Exception{
//		printTestDisabledMessage("bug 23324 ");
		fieldMethodHelper1(new String[]{"f"}, new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	//----
	public void testAddingRequiredMembers0() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers1() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers2() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers3() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= {"m", "y"};
		String[][] expectedMethodSignatures= {new String[0], new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers4() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= {"m", "y"};
		String[][] expectedMethodSignatures= {new String[0], new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers5() throws Exception{
		String[] fieldNames= {"y"};
		String[] methodNames= {};
		String[][] methodSignatures= {};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= {"m"};
		String[][] expectedMethodSignatures= {new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers6() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers7() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers8() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= {"m", "foo"};
		String[][] expectedMethodSignatures= {new String[0], new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers9() throws Exception{
		String[] fieldNames= {"m"};
		String[] methodNames= {};
		String[][] methodSignatures= {};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers10() throws Exception{
		String[] fieldNames= {"m"};
		String[] methodNames= {};
		String[][] methodSignatures= {};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= {"foo"};
		String[][] expectedMethodSignatures= {new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers11() throws Exception{
		String[] fieldNames= {"m"};
		String[] methodNames= {};
		String[][] methodSignatures= {};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= {"foo"};
		String[][] expectedMethodSignatures= {new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testAddingRequiredMembers12() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= {"foo", "m"};
		String[][] expectedMethodSignatures= {new String[0], new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	public void testEnablement0() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement1() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IType typeD= cu.getType("D");
		IMember[] members= {typeB, typeD};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement2() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement3() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement4() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement5() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement6() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement7() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement8() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement9() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement10() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement11() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement12() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement13() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IType typeD= cu.getType("D");
		IMember[] members= {typeB, typeD};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement14() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IType typeD= cu.getType("D");
		IMember[] members= {typeB, typeD};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement15() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IType typeD= cu.getType("D");
		IMember[] members= {typeB, typeD};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement16() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IType typeD= cu.getType("D");
		IMember[] members= {typeB, typeD};
		assertTrue("should be disabled", ! RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement17() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement18() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement19() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement20() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement21() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement22() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement23() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	public void testEnablement24() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeB};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPullUpAvailable(members));
	}

	//------------------ tests -------------

	public void testStaticImports0() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[] {"QS;"}};

			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setSuperclassAsTargetClass(processor);

			processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to pass", result == null || !result.hasError());

			assertEqualLines("A", cuA.getSource(), getFileContents(getOutputTestFileName("A")));
			assertEqualLines("B", cuB.getSource(), getFileContents(getOutputTestFileName("B")));
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}
	}

	public void testStaticImports1() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[] {"QS;"}};

			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setSuperclassAsTargetClass(processor);

			processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to pass", result == null || !result.hasError());

			assertEqualLines("A", cuA.getSource(), getFileContents(getOutputTestFileName("A")));
			assertEqualLines("B", cuB.getSource(), getFileContents(getOutputTestFileName("B")));
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}
	}

	public void testGenerics0() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testGenerics1() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testGenerics2() throws Exception{
		helper1(new String[]{"mmm", "n"}, new String[][]{new String[] {"QT;"}, new String[0]}, true, false, 0);
	}

	public void testGenerics3() throws Exception{
		helper1(new String[]{"mmm", "n"}, new String[][]{new String[] {"QT;"}, new String[0]}, true, true, 0);
	}

	public void testGenerics4() throws Exception{
		printTestDisabledMessage("see bug 75642");

//		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
//		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
//
//		try{
//			String[] methodNames= new String[]{"m"};
//			String[][] signatures= new String[][]{new String[]{"QList<QT;>;"}};
//
//			IType type= getType(cuB, "B");
//			IMethod[] methods= getMethods(type, methodNames, signatures);
//			PullUpRefactoring ref= createRefactoring(methods);
//			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
//			setSuperclassAsTargetClass(ref);
//
//			ref.setMethodsToDelete(getMethods(ref.getMatchingElements(new NullProgressMonitor(), false)));
//
//			RefactoringStatus result= performRefactoring(ref);
//			assertEquals("precondition was supposed to pass", null, result);
//
//			assertEqualLines("A", cuA.getSource(), getFileContents(getOutputTestFileName("A")));
//			assertEqualLines("B", cuB.getSource(), getFileContents(getOutputTestFileName("B")));
//		} finally{
//			performDummySearch();
//			cuA.delete(false, null);
//			cuB.delete(false, null);
//		}
	}

	public void testGenerics5() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[] {"QS;"}};

			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setSuperclassAsTargetClass(processor);

			processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to pass", result == null || !result.hasError());

			assertEqualLines("A", cuA.getSource(), getFileContents(getOutputTestFileName("A")));
			assertEqualLines("B", cuB.getSource(), getFileContents(getOutputTestFileName("B")));
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}
	}

	public void testGenerics6() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testGenerics7() throws Exception{
		printTestDisabledMessage("Disabled because of bug ");

		if (!BUG_91542)
			helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testGenerics8() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testGenerics9() throws Exception{
		printTestDisabledMessage("Disabled because of bug ");

		if (!BUG_91542)
			helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testGenerics10() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testGenerics11() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false, 0);
	}

	public void testGenerics12() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[]{"QT;"}};

			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);

			PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
			Refactoring ref= processor.getRefactoring();

			assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
			setSuperclassAsTargetClass(processor);

			processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to pass", result == null || !result.hasError());

			assertEqualLines("A", cuA.getSource(), getFileContents(getOutputTestFileName("A")));
			assertEqualLines("B", cuB.getSource(), getFileContents(getOutputTestFileName("B")));
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}
	}

	public void testGenerics13() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, true, false, 0);
	}

	public void testGenerics14() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, true, false, 0);
	}

	public void testGenerics15() throws Exception {
		helper1(new String[] { "m"}, new String[][] { new String[0]}, true, false, 0);
	}

	public void testGenericsFail0() throws Exception {
		helper2(new String[] { "m"}, new String[][] { new String[] {"QT;"}}, true, false, 0);
	}

	public void testGenericsFail1() throws Exception {
		helper2(new String[] { "m"}, new String[][] { new String[]{"QS;"}}, true, false, 0);
	}

	public void testGenericsFail2() throws Exception {
		helper2(new String[] { "m"}, new String[][] { new String[]{"QT;"}}, true, false, 0);
	}
}
