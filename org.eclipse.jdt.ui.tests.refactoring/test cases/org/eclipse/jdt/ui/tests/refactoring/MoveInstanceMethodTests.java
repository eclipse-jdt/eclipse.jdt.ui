/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class MoveInstanceMethodTests extends RefactoringTest {

	private static final Class clazz= MoveInstanceMethodTests.class;

	public static final int FIELD= 1;

	public static final int PARAMETER= 0;

	private static final String REFACTORING_PATH= "MoveInstanceMethod/";

	public static void chooseNewTarget(MoveInstanceMethodProcessor processor, int newTargetType, String newTargetName) {
		IVariableBinding target= null;
		IVariableBinding[] targets= processor.getPossibleTargets();
		for (int i= 0; i < targets.length; i++) {
			IVariableBinding candidate= targets[i];
			if (candidate.getName().equals(newTargetName) && typeMatches(newTargetType, candidate)) {
				target= candidate;
				break;
			}
		}
		assertNotNull("Expected new target not available.", target);
		processor.setTarget(target);
	}

	private static IMethod getMethod(ICompilationUnit cu, ISourceRange sourceRange) throws JavaModelException {
		IJavaElement[] jes= cu.codeSelect(sourceRange.getOffset(), sourceRange.getLength());
		if (jes.length != 1 || !(jes[0] instanceof IMethod))
			return null;
		return (IMethod) jes[0];
	}

	public static Test setUpTest(Test someTest) {
		return new Java15Setup(someTest);
	}

	public static Test suite() {
		return new Java15Setup(new TestSuite(clazz));
	}

	private static boolean typeMatches(int newTargetType, IVariableBinding newTarget) {
		return newTargetType == PARAMETER && !newTarget.isField() || newTargetType == FIELD && newTarget.isField();
	}

	private boolean toSucceed;

	public MoveInstanceMethodTests(String name) {
		super(name);
	}

	private ICompilationUnit[] createCUs(String[] qualifiedNames) throws Exception {
		ICompilationUnit[] cus= new ICompilationUnit[qualifiedNames.length];
		for (int i= 0; i < qualifiedNames.length; i++) {
			Assert.isNotNull(qualifiedNames[i]);
			cus[i]= createCUfromTestFile(getRoot().createPackageFragment(getQualifier(qualifiedNames[i]), true, null), getSimpleName(qualifiedNames[i]));
		}
		return cus;
	}

	private void failHelper1(String cuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, boolean inlineDelegator, boolean removeDelegator) throws Exception {
		failHelper1(new String[] { cuQName}, cuQName, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, inlineDelegator, removeDelegator);
	}

	private void failHelper1(String[] cuQNames, int selectionCuIndex, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, String newMethodName, String newTargetName, boolean inlineDelegator, boolean removeDelegator) throws Exception {
		Assert.isTrue(0 <= selectionCuIndex && selectionCuIndex < cuQNames.length);

		toSucceed= false;

		ICompilationUnit[] cus= createCUs(cuQNames);
		ICompilationUnit selectionCu= cus[selectionCuIndex];

		ISourceRange selection= TextRangeUtil.getSelection(selectionCu, startLine, startColumn, endLine, endColumn);
		IMethod method= getMethod(selectionCu, selection);
		assertNotNull(method);
		MoveInstanceMethodProcessor processor= new MoveInstanceMethodProcessor(method, JavaPreferencesSettings.getCodeGenerationSettings(selectionCu.getJavaProject()));
		Refactoring ref= new MoveRefactoring(processor);
		RefactoringStatus result= ref.checkInitialConditions(new NullProgressMonitor());
		if (!result.isOK())
			return;
		else {
			chooseNewTarget(processor, newReceiverType, newReceiverName);

			if (newTargetName != null)
				processor.setTargetName(newTargetName);
			processor.setInlineDelegator(inlineDelegator);
			processor.setRemoveDelegator(removeDelegator);
			processor.setDeprecateDelegates(false);
			if (newMethodName != null)
				processor.setMethodName(newMethodName);

			result.merge(ref.checkFinalConditions(new NullProgressMonitor()));

			assertTrue("precondition checking is expected to fail.", !result.isOK());
		}
	}

	private void failHelper1(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, boolean inlineDelegator, boolean removeDelegator) throws Exception {
		int selectionCuIndex= firstIndexOf(selectionCuQName, cuQNames);
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		failHelper1(cuQNames, selectionCuIndex, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, null, null, inlineDelegator, removeDelegator);
	}

	private void failHelper2(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, String originalReceiverParameterName, boolean inlineDelegator, boolean removeDelegator) throws Exception {
		int selectionCuIndex= firstIndexOf(selectionCuQName, cuQNames);
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		failHelper1(cuQNames, selectionCuIndex, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, null, originalReceiverParameterName, inlineDelegator, removeDelegator);
	}

	private int firstIndexOf(String one, String[] others) {
		for (int i= 0; i < others.length; i++)
			if (one == null && others[i] == null || one.equals(others[i]))
				return i;
		return -1;
	}

	private String getQualifier(String qualifiedName) {
		int dot= qualifiedName.lastIndexOf('.');
		return qualifiedName.substring(0, dot != -1 ? dot : 0);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH + successPath();
	}

	private String getSimpleName(String qualifiedName) {
		return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
	}

	private void helper1(String[] cuQNames, int selectionCuIndex, int startLine, int startColumn, int endLine, int endColumn, int newTargetType, String newTargetName, String newMethodName, boolean inlineDelegator, boolean removeDelegator) throws Exception {
		Assert.isTrue(0 <= selectionCuIndex && selectionCuIndex < cuQNames.length);

		toSucceed= true;

		ICompilationUnit[] cus= createCUs(cuQNames);
		ICompilationUnit selectionCu= cus[selectionCuIndex];

		ISourceRange selection= TextRangeUtil.getSelection(selectionCu, startLine, startColumn, endLine, endColumn);
		IMethod method= getMethod(selectionCu, selection);
		assertNotNull(method);
		MoveInstanceMethodProcessor processor= new MoveInstanceMethodProcessor(method, JavaPreferencesSettings.getCodeGenerationSettings(selectionCu.getJavaProject()));
		Refactoring ref= new MoveRefactoring(processor);

		assertNotNull("refactoring should be created", ref);
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", preconditionResult.isOK());

		chooseNewTarget(processor, newTargetType, newTargetName);

		processor.setInlineDelegator(inlineDelegator);
		processor.setRemoveDelegator(removeDelegator);
		processor.setDeprecateDelegates(false);
		if (newMethodName != null)
			processor.setMethodName(newMethodName);

		preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));

		assertTrue("precondition was supposed to pass", !preconditionResult.hasError());

		performChange(ref, false);

		for (int i= 0; i < cus.length; i++) {
			String outputTestFileName= getOutputTestFileName(getSimpleName(cuQNames[i]));
			assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cus[i].getSource());
		}
	}

	private void helper1(String[] cuQNames, int selectionCuIndex, int startLine, int startColumn, int endLine, int endColumn, int newTargetType, String newTargetName, String newMethodName, boolean inlineDelegator, boolean removeDelegator, boolean deprecate) throws Exception {
		Assert.isTrue(0 <= selectionCuIndex && selectionCuIndex < cuQNames.length);

		toSucceed= true;

		ICompilationUnit[] cus= createCUs(cuQNames);
		ICompilationUnit selectionCu= cus[selectionCuIndex];

		ISourceRange selection= TextRangeUtil.getSelection(selectionCu, startLine, startColumn, endLine, endColumn);
		IMethod method= getMethod(selectionCu, selection);
		assertNotNull(method);
		MoveInstanceMethodProcessor processor= new MoveInstanceMethodProcessor(method, JavaPreferencesSettings.getCodeGenerationSettings(selectionCu.getJavaProject()));
		Refactoring ref= new MoveRefactoring(processor);

		assertNotNull("refactoring should be created", ref);
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", preconditionResult.isOK());

		chooseNewTarget(processor, newTargetType, newTargetName);

		processor.setInlineDelegator(inlineDelegator);
		processor.setRemoveDelegator(removeDelegator);
		processor.setDeprecateDelegates(deprecate);
		if (newMethodName != null)
			processor.setMethodName(newMethodName);

		preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));

		assertTrue("precondition was supposed to pass", !preconditionResult.hasError());

		performChange(ref, false);

		for (int i= 0; i < cus.length; i++) {
			String outputTestFileName= getOutputTestFileName(getSimpleName(cuQNames[i]));
			assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cus[i].getSource());
		}
	}

	private void helper1(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, boolean inlineDelegator, boolean removeDelegator) throws Exception {
		int selectionCuIndex= firstIndexOf(selectionCuQName, cuQNames);
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		helper1(cuQNames, selectionCuIndex, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, null, inlineDelegator, removeDelegator);
	}

	private void helper1(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName, boolean inlineDelegator, boolean removeDelegator, boolean deprecate) throws Exception {
		int selectionCuIndex= firstIndexOf(selectionCuQName, cuQNames);
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		helper1(cuQNames, selectionCuIndex, startLine, startColumn, endLine, endColumn, newReceiverType, newReceiverName, null, inlineDelegator, removeDelegator, deprecate);
	}

	private String successPath() {
		return toSucceed ? "/canMove/" : "/cannotMove/";
	}

	// --- TESTS

	// Move mA1 to parameter b, do not inline delegator
	public void test0() throws Exception {
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 11, 17, 11, 20, PARAMETER, "b", false, false);
	}

	// Move mA1 to parameter b, inline delegator
	public void test1() throws Exception {
		// printTestDisabledMessage("not implemented yet");
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 11, 17, 11, 20, PARAMETER, "b", true, false);
	}

	// multiple parameters, some left of new receiver parameter, some right of it,
	// "this" is NOT passed as argument, (since it's not used in the method)
	public void test10() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 14, 17, 14, 17, PARAMETER, "b", false, false);
	}

	// move to field, method has parameters, choice of fields, some non-class type fields
	// ("this" is passed as argument)
	public void test11() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 17, 17, 17, 17, FIELD, "fB", false, false);
	}

	// move to field - do not pass 'this' because it's unneeded
	public void test12() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 14, 17, 14, 20, FIELD, "fB", false, false);
	}

	// junit case
	public void test13() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false);
	}

	// simplified junit case
	public void test14() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false);
	}

	// move to type in same cu
	public void test15() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=40120
		helper1(new String[] { "p.A"}, "p.A", 17, 18, 17, 18, PARAMETER, "s", false, false);
	}

	// move to inner type in same cu
	public void test16() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=40120
		helper1(new String[] { "p.B"}, "p.B", 15, 17, 15, 22, PARAMETER, "s", false, false);
	}

	// don't generate parameter for unused field (bug 38310)
	public void test17() throws Exception {
		helper1(new String[] { "p.Shape", "p.Rectangle"}, "p.Shape", 11, 16, 11, 20, FIELD, "fBounds", false, false);
	}

	// generate parameter for used field (bug 38310)
	public void test18() throws Exception {
		helper1(new String[] { "p.Shape", "p.Rectangle"}, "p.Shape", 17, 22, 17, 22, FIELD, "fInnerBounds", false, false);
	}

	// generate parameter for used field (bug 38310)
	public void test19() throws Exception {
		helper1(new String[] { "p.Shape", "p.Rectangle"}, "p.Shape", 22, 20, 22, 33, PARAMETER, "rect", false, false);
	}

	// // Move mA1 to parameter b, inline delegator, remove delegator
	public void test2() throws Exception {
		// printTestDisabledMessage("not implemented yet");
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 12, 17, 12, 20, PARAMETER, "b", true, true);
	}

	// Can move if "super" is used in inner class
	public void test20() throws Exception {
		helper1(new String[] { "p.A", "p.B", "p.StarDecorator"}, "p.A", 14, 17, 14, 22, PARAMETER, "b", false, false);
	}

	// Arguments of method calls preserved in moved body (bug 41468)
	public void test21() throws Exception {
		helper1(new String[] { "p.A", "p.Second"}, "p.A", 5, 17, 5, 22, FIELD, "s", false, false);
	}

	// arguments of method calls preserved in moved body (bug 41468),
	// use "this" instead of field (bug 38310)
	public void test22() throws Exception {
		helper1(new String[] { "p.A", "p.Second"}, "p.A", 5, 17, 5, 22, FIELD, "s", false, false);
	}

	// "this"-qualified field access: this.s -> this (bug 41597)
	public void test23() throws Exception {
		helper1(new String[] { "p.A", "p.Second"}, "p.A", 5, 17, 5, 22, FIELD, "s", false, false);
	}

	// move local class (41530)
	public void test24() throws Exception {
		helper1(new String[] { "p1.A", "p1.B", "p1.StarDecorator"}, "p1.A", 9, 17, 9, 22, PARAMETER, "b", false, false);
	}

	// extended junit case
	public void test25() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 4, 20, 4, 23, PARAMETER, "test", false, false);
	}

	// extended junit case with generics (bug 77653)
	public void test26() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false);
	}

	// extended junit case with generics and deprecation message
	public void test27() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false, true);
	}

	// extended junit case with generics, enums and deprecation message
	public void test28() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false, true);
	}

	// extended junit case with generics, enums and deprecation message
	public void test29() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false, true);
	}

	// extended junit case with generics, enums, static imports and deprecation message
	public void test30() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 10, 21, 10, 21, PARAMETER, "test", false, false, true);
	}

	// extended junit case with generics, enums, static imports and deprecation message
	public void test31() throws Exception {
		printTestDisabledMessage("disabled due to missing support for statically imported methods");
//		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 10, 21, 10, 21, PARAMETER, "test", false, false, true);
	}

	public void test32() throws Exception {
		helper1(new String[] { "p1.A"}, "p1.A", 9, 25, 9, 26, PARAMETER, "p", true, true);
	}

	// Test visibility of a target field is not affected.
	public void test33() throws Exception {
		helper1(new String[] { "p.Foo", "p.Bar" }, "p.Foo", 6, 18, 6, 21, FIELD, "_bar", false, false);
	}

	// Test visibility of target field is changed to public
	// in case a caller is in another package (bug 117465).
	public void test34() throws Exception {
		helper1(new String[] { "test1.TestTarget", "test1.Test1", "test2.Test2"}, "test1.Test1", 3, 21, 3, 33, FIELD, "target", true, true);
	}

	// Test visibility of target field is changed to default
	// in case a caller is in the same package (bug 117465).
	public void test35() throws Exception {
		helper1(new String[] { "test1.TestTarget", "test1.Test1", "test1.Test2"}, "test1.Test1", 3, 21, 3, 33, FIELD, "target", true, true);
	}

	// Test search engine for secondary types (bug 108030).
	public void test36() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 9, 17, 9, 27, FIELD, "fB", false, false);
	}

	// Test name conflicts in the moved method between fields and parameters (bug 227876)
	public void test37() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 4, 17, 4, 42, FIELD, "destination", true, true);
	}

	// Test problem with parameter order (bug 165697)
	public void test38() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 4, 17, 4, 35, FIELD, "target", true, true);
	}

	// Test problem with qualified accesses (bug 149316)
	public void test39() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 4, 13, 4, 25, PARAMETER, "p", true, true);
	}

	// Test problem with qualified accesses (bug 149316)
	public void test40() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 4, 13, 4, 25, PARAMETER, "p", true, true);
	}

	// Move mA1 to field fB, do not inline delegator
	public void test3() throws Exception {
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 9, 17, 9, 20, FIELD, "fB", false, false);
	}

	// // Move mA1 to field fB, inline delegator, remove delegator
	public void test4() throws Exception {
		// printTestDisabledMessage("not implemented yet");
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 9, 17, 9, 20, FIELD, "fB", true, true);
	}

	// Move mA1 to field fB, unqualified static member references are qualified
	public void test5() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 15, 19, 15, 19, FIELD, "fB", false, false);
	}

	// class qualify referenced type name to top level, original receiver not used in method
	public void test6() throws Exception {
		helper1(new String[] { "p1.Nestor", "p2.B"}, "p1.Nestor", 11, 17, 11, 17, PARAMETER, "b", false, false);
	}

	public void test7() throws Exception {
		helper1(new String[] { "p1.A", "p2.B", "p3.N1"}, "p1.A", 8, 17, 8, 18, PARAMETER, "b", false, false);
	}

	// access to fields, non-void return type
	public void test8() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 15, 19, 15, 20, PARAMETER, "b", false, false);
	}

	// multiple parameters, some left of new receiver parameter, some right of it,
	// "this" is passed as argument
	public void test9() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 17, 6, 17, PARAMETER, "b", false, false);
	}

	// Cannot move interface method declaration
	public void testFail0() throws Exception {
		failHelper1("p1.IA", 5, 17, 5, 20, PARAMETER, "b", true, true);
	}

	// Cannot move abstract method declaration
	public void testFail1() throws Exception {
		failHelper1("p1.A", 5, 26, 5, 29, PARAMETER, "b", true, true);
	}

	// Cannot move method if there's no new potential receiver
	public void testFail10() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 8, 17, 8, 20, PARAMETER, "b", true, true);
	}

	// Cannot move method - parameter name conflict
	public void testFail11() throws Exception {
		failHelper2(new String[] { "p1.A", "p2.B"}, "p1.A", 7, 17, 7, 20, PARAMETER, "b", "a", true, true);
	}

	// Cannot move method if there's no new potential receiver (because of null bindings here)
	public void testFail12() throws Exception {
		// printTestDisabledMessage("bug 39871");
		failHelper1(new String[] { "p1.A"}, "p1.A", 5, 10, 5, 16, PARAMETER, "b", true, true);
	}

	// Cannot move method - annotations are not supported
	public void testFail13() throws Exception {
		printTestDisabledMessage("disabled - jcore does not have elements for annotation members");
//		failHelper2(new String[] { "p1.A", "p2.B"}, "p1.A", 5, 12, 5, 13, PARAMETER, "b", "a", true, true);
	}

	// Cannot move static method
	public void testFail2() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 23, 6, 24, PARAMETER, "b", true, true);
	}

	// Cannot move native method
	public void testFail3() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 23, 6, 24, PARAMETER, "b", true, true);
	}

	// Cannot move method that references "super"
	public void testFail4() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 11, 20, 11, 21, PARAMETER, "b", true, true);
	}

	// Cannot move method that references an enclosing instance
	public void testFail5() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 8, 21, 8, 21, PARAMETER, "b", true, true);
	}

	// Cannot move potentially directly recursive method
	public void testFail6() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 16, 6, 17, PARAMETER, "b", true, true);
	}

	// Cannot move synchronized method
	public void testFail8() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 29, 6, 29, PARAMETER, "b", true, true);
	}

	// Cannot move method if there's no new potential receiver
	public void testFail9() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 7, 17, 7, 20, PARAMETER, "b", true, true);
	}
}
