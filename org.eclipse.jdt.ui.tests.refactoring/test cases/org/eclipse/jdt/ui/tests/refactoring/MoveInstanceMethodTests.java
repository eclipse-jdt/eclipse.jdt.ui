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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring.INewReceiver;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

public class MoveInstanceMethodTests extends RefactoringTest {

	private static final Class clazz= MoveInstanceMethodTests.class;
	private static final String REFACTORING_PATH= "MoveInstanceMethod/";

	private static final int PARAMETER= 0;
	private static final int FIELD= 1;

	private boolean toSucceed;

	public MoveInstanceMethodTests(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH+ successPath();
	}

	private String successPath() {
		return toSucceed ? "/canMove/" : "/cannotMove/";
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	private String getSimpleName(String qualifiedName) {
		return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
	}

	private String getQualifier(String qualifiedName) {
		int dot= qualifiedName.lastIndexOf('.');
		return qualifiedName.substring(0, dot != -1 ? dot : 0);
	}

	private ICompilationUnit[] createCUs(String[] qualifiedNames) throws Exception {
		ICompilationUnit[] cus= new ICompilationUnit[qualifiedNames.length];
		for(int i= 0; i < qualifiedNames.length; i++) {
			Assert.isNotNull(qualifiedNames[i]);

			cus[i]= createCUfromTestFile(getRoot().createPackageFragment(getQualifier(qualifiedNames[i]), true, null),
													  getSimpleName(qualifiedNames[i]));
		}
		return cus;
	}

	private int firstIndexOf(String one, String[] others) {
		for(int i= 0; i < others.length; i++)
			if(one == null && others[i] == null || one.equals(others[i]))
				return i;
		return -1;
	}
	
	private void helper1(String cuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, boolean inlineDelegator, boolean removeDelegator) throws Exception {
		helper1(new String[] {cuQName}, cuQName, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, inlineDelegator, removeDelegator);
	}
	
	private void helper1(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, boolean inlineDelegator, boolean removeDelegator) throws Exception {
		int selectionCuIndex= firstIndexOf(selectionCuQName, cuQNames);
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		helper1(cuQNames, selectionCuIndex, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, null, inlineDelegator, removeDelegator);
	}
	
	private static void chooseNewReceiver(MoveInstanceMethodRefactoring ref, int newReceiverType, String newReceiverName) {
		INewReceiver chosen= null;
		INewReceiver[] possibleNewReceivers= ref.getPossibleNewReceivers();
		for(int i= 0; i < possibleNewReceivers.length; i++)  {
			INewReceiver candidate= possibleNewReceivers[i];
			if(   candidate.getName().equals(newReceiverName)
			   && typeMatches(newReceiverType, candidate)) {
				assertNull(chosen);
				chosen= candidate;
			}
		}
		assertNotNull("Expected new receiver not available.", chosen);
		ref.chooseNewReceiver(chosen);		
	}
	
	private static boolean typeMatches(int newReceiverType, INewReceiver newReceiver) {
		return    newReceiverType == PARAMETER && newReceiver.isParameter()
		        || newReceiverType == FIELD && newReceiver.isField();
	}
	
	private void helper1(String[] cuQNames, int selectionCuIndex, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, String newMethodName, boolean inlineDelegator, boolean removeDelegator) throws Exception{
		Assert.isTrue(0 <= selectionCuIndex && selectionCuIndex < cuQNames.length);

		toSucceed= true;

		ICompilationUnit[] cus= createCUs(cuQNames);
		ICompilationUnit selectionCu= cus[selectionCuIndex];

		ISourceRange selection= TextRangeUtil.getSelection(selectionCu, startLine, startColumn, endLine, endColumn);
		IMethod method= getMethod(selectionCu, selection);
		assertNotNull(method);
		MoveInstanceMethodRefactoring ref= MoveInstanceMethodRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings());
		
		assertNotNull("refactoring should be created", ref);
		RefactoringStatus preconditionResult= ref.checkActivation(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", preconditionResult.isOK());

		chooseNewReceiver(ref, newReceiverType, newReceiverName);
		
		ref.setRemoveDelegator(removeDelegator);
		ref.setInlineDelegator(inlineDelegator);
		if(newMethodName != null)
			ref.setNewMethodName(newMethodName);

		preconditionResult.merge(ref.checkInput(new NullProgressMonitor()));

		assertTrue("precondition was supposed to pass",preconditionResult.isOK());

		performChange(ref);

		for(int i= 0; i < cus.length; i++) {
			String outputTestFileName= getOutputTestFileName(getSimpleName(cuQNames[i]));
			assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cus[i].getSource());
		}
	}

	private void failHelper1(String cuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, boolean inlineDelegator, boolean removeDelegator, int errorCode) throws Exception {
		failHelper1(new String[] {cuQName}, cuQName, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, inlineDelegator, removeDelegator, errorCode);
	}
	private void failHelper1(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, boolean inlineDelegator, boolean removeDelegator, int errorCode) throws Exception {
		int selectionCuIndex= firstIndexOf(selectionCuQName, cuQNames);
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		failHelper1(cuQNames, selectionCuIndex, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, null, null, inlineDelegator, removeDelegator, errorCode);
	}
	private void failHelper2(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, String originalReceiverParameterName, boolean inlineDelegator, boolean removeDelegator, int errorCode) throws Exception {
		int selectionCuIndex= firstIndexOf(selectionCuQName, cuQNames);
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		failHelper1(cuQNames, selectionCuIndex, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, null, originalReceiverParameterName, inlineDelegator, removeDelegator, errorCode);
	}
	private void failHelper1(String[] cuQNames, int selectionCuIndex, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, String newMethodName, String originalReceiverParameterName, boolean inlineDelegator, boolean removeDelegator, int errorCode) throws Exception {
		Assert.isTrue(0 <= selectionCuIndex && selectionCuIndex < cuQNames.length);

		toSucceed= false;

		ICompilationUnit[] cus= createCUs(cuQNames);
		ICompilationUnit selectionCu= cus[selectionCuIndex];

		ISourceRange selection= TextRangeUtil.getSelection(selectionCu, startLine, startColumn, endLine, endColumn);
		IMethod method= getMethod(selectionCu, selection);
		assertNotNull(method);
		MoveInstanceMethodRefactoring ref= MoveInstanceMethodRefactoring.create(method,
																									JavaPreferencesSettings.getCodeGenerationSettings());
		if (ref == null) {
			assertTrue(errorCode != 0);
		} else  {
			RefactoringStatus result= ref.checkActivation(new NullProgressMonitor());

			if(!result.isOK()) {
				assertEquals(errorCode, result.getEntryMatchingSeverity(RefactoringStatus.ERROR).getCode());
				return;
			} else {
				chooseNewReceiver(ref, newReceiverType, newReceiverName);
	
				if (originalReceiverParameterName != null)
					ref.setOriginalReceiverParameterName(originalReceiverParameterName);
				ref.setRemoveDelegator(removeDelegator);			
				ref.setInlineDelegator(inlineDelegator);
				if(newMethodName != null)
					ref.setNewMethodName(newMethodName);
	
				result.merge(ref.checkInput(new NullProgressMonitor()));
	
				assertTrue("precondition checking is expected to fail.", !result.isOK());
				assertEquals(errorCode, result.getEntryMatchingSeverity(RefactoringStatus.ERROR).getCode());
			}
		}
	}	

	private static IMethod getMethod(ICompilationUnit cu, ISourceRange sourceRange) throws JavaModelException {
		IJavaElement[] jes= cu.codeSelect(sourceRange.getOffset(), sourceRange.getLength());
		if (jes.length != 1 || ! (jes[0] instanceof IMethod))
			return null;
		return (IMethod)jes[0];
	}
	
	//--- TESTS
	
	// Move mA1 to parameter b, do not inline delegator	
	public void test0() throws Exception {
		helper1(new String[] {"p1.A", "p2.B", "p3.C"}, "p1.A", 7, 17, 7, 20, PARAMETER, "b", false, false);
	}
	
	// Move mA1 to parameter b, inline delegator
	public void test1() throws Exception {
		printTestDisabledMessage("not implemented yet");
//		helper1(new String[] {"p1.A", "p2.B", "p3.C"}, "p1.A", 7, 17, 7, 20, PARAMETER, "b", true, false);
	}

//	// Move mA1 to parameter b, inline delegator, remove delegator
	public void test2() throws Exception {
		printTestDisabledMessage("not implemented yet");
//		helper1(new String[] {"p1.A", "p2.B", "p3.C"}, "p1.A", 7, 17, 7, 20, PARAMETER, "b", true, true);
	}
	
	// Move mA1 to field fB, do not inline delegator
	public void test3() throws Exception {
		helper1(new String[] {"p1.A", "p2.B", "p3.C"}, "p1.A", 9, 17, 9, 20, FIELD, "fB", false, false);
	}
	
//	// Move mA1 to field fB, inline delegator, remove delegator
	public void test4() throws Exception {
		printTestDisabledMessage("not implemented yet");		
//		helper1(new String[] {"p1.A", "p2.B", "p3.C"}, "p1.A", 9, 17, 9, 20, FIELD, "fB", true, true);
	}
	
	// Move mA1 to field fB, unqualified static member references are qualified
	public void test5() throws Exception {
		helper1(new String[] {"p1.A", "p2.B"}, "p1.A", 15, 19, 15, 19, FIELD, "fB", false, false);	
	}
	
	// class qualify referenced type name to top level, original receiver not used in method
	public void test6() throws Exception {
		helper1(new String[] {"p1.Nestor", "p2.B"}, "p1.Nestor", 11, 17, 11, 17, PARAMETER, "b", false, false);
	}	
	
	public void test7() throws Exception {
		helper1(new String[] {"p1.A", "p2.B", "p3.N1"}, "p1.A", 8, 17, 8, 18, PARAMETER, "b", false, false);
	}
	
	// access to fields, non-void return type
	public void test8() throws Exception {
		helper1(new String[] {"p1.A", "p2.B"}, "p1.A", 9, 19, 9, 20, PARAMETER, "b", false, false);
	}
	
	// multiple parameters, some left of new receiver parameter, some right of it,
	// "this" is passed as first argument
	public void test9() throws Exception {
		helper1(new String[] {"p1.A", "p2.B"}, "p1.A", 6, 17, 6, 17, PARAMETER, "b", false, false);
	}	
	
	// multiple parameters, some left of new receiver parameter, some right of it,
	// "this" is NOT passed as first argument, (since it's not used in the method)
	public void test10() throws Exception {
		helper1(new String[] {"p1.A", "p2.B"}, "p1.A", 6, 17, 6, 17, PARAMETER, "b", false, false);
	}
	
	//move to field, method has parameters, choice of fields, some non-class type fields
	// ("this" is passed as first argument)
	public void test11() throws Exception {
		helper1(new String[] {"p1.A", "p2.B"}, "p1.A", 11, 17, 11, 17, FIELD, "fB", false, false);
	}	

	//move to field - do not pass 'this' because it's unneeded
	public void test12() throws Exception {
		helper1(new String[] {"p1.A", "p2.B"}, "p1.A", 8, 17, 8, 20, FIELD, "fB", false, false);
	}	

	//junit case
	public void test13() throws Exception {
		helper1(new String[] {"p1.TR", "p1.TC", "p1.P"}, "p1.TR", 4, 20, 4, 23, PARAMETER, "test", false, false);
	}	

	//simplified junit case
	public void test14() throws Exception {
		helper1(new String[] {"p1.TR", "p1.TC"}, "p1.TR", 4, 20, 4, 23, PARAMETER, "test", false, false);
	}	
	
	//move to type in same cu
	public void test15() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=40120
		helper1(new String[] {"p.A"}, "p.A", 13, 18, 13, 18, PARAMETER, "s", false, false);
	}	

	//move to inner type in same cu
	public void test16() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=40120
		helper1(new String[] {"p.B"}, "p.B", 11, 17, 11, 22, PARAMETER, "s", false, false);
	}

	//don't generate parameter for unused field (bug 38310)
	public void test17() throws Exception {
		helper1(new String[] {"p.Shape", "p.Rectangle"}, "p.Shape", 7, 16, 7, 20, FIELD, "fBounds", false, false);
	}	

	//generate parameter for used field (bug 38310)
	public void test18() throws Exception {
		helper1(new String[] {"p.Shape", "p.Rectangle"}, "p.Shape", 13, 22, 13, 22, FIELD, "fInnerBounds", false, false);
	}	

	//generate parameter for used field (bug 38310)
	public void test19() throws Exception {
		helper1(new String[] {"p.Shape", "p.Rectangle"}, "p.Shape", 17, 20, 17, 33, PARAMETER, "rect", false, false);
	}	
	
	// Can move if "super" is used in inner class 
	public void test20() throws Exception {
		helper1(new String[] {"p.A", "p.B", "p.StarDecorator"}, "p.A", 10, 17, 10, 22, PARAMETER, "b", false, false);
	}	

	// Arguments of method calls preserved in moved body (bug 41468)
	public void test21() throws Exception {
		helper1(new String[] {"p.A", "p.Second"}, "p.A", 5, 17, 5, 22, FIELD, "s", false, false);
	}

	// arguments of method calls preserved in moved body (bug 41468),
	// use "this" instead of field (bug 38310)
	public void test22() throws Exception {
		helper1(new String[] {"p.A", "p.Second"}, "p.A", 5, 17, 5, 22, FIELD, "s", false, false);
	}

	// "this"-qualified field access: this.s -> this (bug 41597)
	public void test23() throws Exception {
		helper1(new String[] {"p.A", "p.Second"}, "p.A", 5, 17, 5, 22, FIELD, "s", false, false);
	}

	// move local class (41530)
	public void test24() throws Exception {
		helper1(new String[] {"p1.A", "p1.B", "p1.StarDecorator"}, "p1.A", 9, 17, 9, 22, PARAMETER, "b", false, false);
	}


	// Cannot move interface method declaration
	public void testFail0() throws Exception {
		failHelper1("p1.IA", 5, 17, 5, 20, PARAMETER, "b", true, true, RefactoringStatusCodes.SELECT_METHOD_IMPLEMENTATION);	
	}
	
	// Cannot move abstract method declaration
	public void testFail1() throws Exception {
		failHelper1("p1.A", 5, 26, 5, 29, PARAMETER, "b", true, true, RefactoringStatusCodes.SELECT_METHOD_IMPLEMENTATION);
	}
	
	// Cannot move static method
	public void testFail2() throws Exception {
		failHelper1(new String[] {"p1.A", "p2.B"}, "p1.A", 6, 23, 6, 24, PARAMETER, "b", true, true, RefactoringStatusCodes.CANNOT_MOVE_STATIC);
	}
	
	// Cannot move native method
	public void testFail3() throws Exception {
		failHelper1(new String[] {"p1.A", "p2.B"}, "p1.A", 6, 23, 6, 24, PARAMETER, "b", true, true, RefactoringStatusCodes.CANNOT_MOVE_NATIVE);
	}
	
	// Cannot move method that references "super"
	public void testFail4() throws Exception {
		failHelper1(new String[] {"p1.A", "p2.B"}, "p1.A", 11, 20, 11, 21, PARAMETER, "b", true, true, RefactoringStatusCodes.SUPER_REFERENCES_NOT_ALLOWED);
	}
	
	// Cannot move method that references an enclosing instance
	public void testFail5() throws Exception {
		failHelper1(new String[] {"p1.A", "p2.B"}, "p1.A", 8, 21, 8, 21, PARAMETER, "b", true, true, RefactoringStatusCodes.ENCLOSING_INSTANCE_REFERENCES_NOT_ALLOWED);
	}
	
	// Cannot move potentially directly recursive method
	public void testFail6() throws Exception {
		failHelper1(new String[] {"p1.A", "p2.B"}, "p1.A", 6, 16, 6, 17, PARAMETER, "b", true, true, RefactoringStatusCodes.CANNOT_MOVE_RECURSIVE);
	}
	
	// Cannot move to local class
	public void testFail7() throws Exception {
		printTestDisabledMessage("not implemented yet - jcore does not have elements for local types");
//		failHelper1("p1.A", 9, 25, 9, 26, PARAMETER, "p", true, true, RefactoringStatusCodes.CANNOT_MOVE_TO_LOCAL);
	}		

	// Cannot move synchronized method
	public void testFail8() throws Exception {
		failHelper1(new String[] {"p1.A", "p2.B"}, "p1.A", 6, 29, 6, 29, PARAMETER, "b", true, true, RefactoringStatusCodes.CANNOT_MOVE_SYNCHRONIZED);
	}

	// Cannot move method if there's no new potential receiver
	public void testFail9() throws Exception {
		failHelper1(new String[] {"p1.A", "p2.B", "p3.C"}, "p1.A", 7, 17, 7, 20, PARAMETER, "b", true, true, RefactoringStatusCodes.NO_NEW_RECEIVERS);	
	}

	// Cannot move method if there's no new potential receiver
	public void testFail10() throws Exception {
		failHelper1(new String[] {"p1.A", "p2.B", "p3.C"}, "p1.A", 8, 17, 8, 20, PARAMETER, "b", true, true, RefactoringStatusCodes.NO_NEW_RECEIVERS);	
	}
	
	// Cannot move method - parameter name conflict
	public void testFail11() throws Exception {
		failHelper2(new String[] {"p1.A", "p2.B"}, "p1.A", 7, 17, 7, 20, PARAMETER, "b", "a", true, true, RefactoringStatusCodes.PARAM_NAME_ALREADY_USED);	
	}

	// Cannot move method if there's no new potential receiver (because of null bindings here)
	public void testFail12() throws Exception {
//		printTestDisabledMessage("bug 39871");
		failHelper1(new String[] {"p1.A"}, "p1.A", 5, 10, 5, 16, PARAMETER, "b", true, true, RefactoringStatusCodes.NO_NEW_RECEIVERS);	
	}

}
