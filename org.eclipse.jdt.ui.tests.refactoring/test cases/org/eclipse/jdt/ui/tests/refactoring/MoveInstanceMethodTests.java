/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [move method] super method invocation does not compile after refactoring - https://bugs.eclipse.org/356687
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [move method] Move method with static imported method calls introduces compiler error - https://bugs.eclipse.org/217753
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [move method] Wrong detection of duplicate methods (can result in compile errors) - https://bugs.eclipse.org/404477
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [move method] Annotation error in applying move-refactoring to inherited methods - https://bugs.eclipse.org/404471
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

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
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d5Setup;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class MoveInstanceMethodTests extends GenericRefactoringTest {
	public static final int FIELD= 1;

	public static final int PARAMETER= 0;

	private static final String REFACTORING_PATH= "MoveInstanceMethod/";

	public MoveInstanceMethodTests() {
		this.rts= new Java1d5Setup();
	}

	protected MoveInstanceMethodTests(RefactoringTestSetup rts) {
		super(rts);
	}

	public static void chooseNewTarget(MoveInstanceMethodProcessor processor, int newTargetType, String newTargetName) {
		IVariableBinding target= null;
		for (IVariableBinding candidate : processor.getPossibleTargets()) {
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


	private static boolean typeMatches(int newTargetType, IVariableBinding newTarget) {
		return newTargetType == PARAMETER && !newTarget.isField() || newTargetType == FIELD && newTarget.isField();
	}

	private boolean toSucceed;

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

			assertFalse("precondition checking is expected to fail.", result.isOK());
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

	@Override
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

		assertFalse("precondition was supposed to pass", preconditionResult.hasError());

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

		assertFalse("precondition was supposed to pass", preconditionResult.hasError());

		performChange(ref, false);

		for (int i= 0; i < cus.length; i++) {
			String outputTestFileName= getOutputTestFileName(getSimpleName(cuQNames[i]));
			assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cus[i].getSource());
		}
	}

	protected void helper1(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, int newReceiverType, String newReceiverName,
			boolean inlineDelegator, boolean removeDelegator) throws Exception {
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
	@Test
	public void test0() throws Exception {
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 11, 17, 11, 20, PARAMETER, "b", false, false);
	}

	// Move mA1 to parameter b, inline delegator
	@Test
	public void test1() throws Exception {
		// printTestDisabledMessage("not implemented yet");
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 11, 17, 11, 20, PARAMETER, "b", true, false);
	}

	// multiple parameters, some left of new receiver parameter, some right of it,
	// "this" is NOT passed as argument, (since it's not used in the method)
	@Test
	public void test10() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 14, 17, 14, 17, PARAMETER, "b", false, false);
	}

	// move to field, method has parameters, choice of fields, some non-class type fields
	// ("this" is passed as argument)
	@Test
	public void test11() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 17, 17, 17, 17, FIELD, "fB", false, false);
	}

	// move to field - do not pass 'this' because it's unneeded
	@Test
	public void test12() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 14, 17, 14, 20, FIELD, "fB", false, false);
	}

	// junit case
	@Test
	public void test13() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false);
	}

	// simplified junit case
	@Test
	public void test14() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false);
	}

	// move to type in same cu
	@Test
	public void test15() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=40120
		helper1(new String[] { "p.A"}, "p.A", 17, 18, 17, 18, PARAMETER, "s", false, false);
	}

	// move to inner type in same cu
	@Test
	public void test16() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=40120
		helper1(new String[] { "p.B"}, "p.B", 15, 17, 15, 22, PARAMETER, "s", false, false);
	}

	// don't generate parameter for unused field (bug 38310)
	@Test
	public void test17() throws Exception {
		helper1(new String[] { "p.Shape", "p.Rectangle"}, "p.Shape", 11, 16, 11, 20, FIELD, "fBounds", false, false);
	}

	// generate parameter for used field (bug 38310)
	@Test
	public void test18() throws Exception {
		helper1(new String[] { "p.Shape", "p.Rectangle"}, "p.Shape", 17, 22, 17, 22, FIELD, "fInnerBounds", false, false);
	}

	// generate parameter for used field (bug 38310)
	@Test
	public void test19() throws Exception {
		helper1(new String[] { "p.Shape", "p.Rectangle"}, "p.Shape", 22, 20, 22, 33, PARAMETER, "rect", false, false);
	}

	// // Move mA1 to parameter b, inline delegator, remove delegator
	@Test
	public void test2() throws Exception {
		// printTestDisabledMessage("not implemented yet");
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 12, 17, 12, 20, PARAMETER, "b", true, true);
	}

	// Can move if "super" is used in inner class
	@Test
	public void test20() throws Exception {
		helper1(new String[] { "p.A", "p.B", "p.StarDecorator"}, "p.A", 14, 17, 14, 22, PARAMETER, "b", false, false);
	}

	// Arguments of method calls preserved in moved body (bug 41468)
	@Test
	public void test21() throws Exception {
		helper1(new String[] { "p.A", "p.Second"}, "p.A", 5, 17, 5, 22, FIELD, "s", false, false);
	}

	// arguments of method calls preserved in moved body (bug 41468),
	// use "this" instead of field (bug 38310)
	@Test
	public void test22() throws Exception {
		helper1(new String[] { "p.A", "p.Second"}, "p.A", 5, 17, 5, 22, FIELD, "s", false, false);
	}

	// "this"-qualified field access: this.s -> this (bug 41597)
	@Test
	public void test23() throws Exception {
		helper1(new String[] { "p.A", "p.Second"}, "p.A", 5, 17, 5, 22, FIELD, "s", false, false);
	}

	// move local class (41530)
	@Test
	public void test24() throws Exception {
		helper1(new String[] { "p1.A", "p1.B", "p1.StarDecorator"}, "p1.A", 9, 17, 9, 22, PARAMETER, "b", false, false);
	}

	// extended junit case
	@Test
	public void test25() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 4, 20, 4, 23, PARAMETER, "test", false, false);
	}

	// extended junit case with generics (bug 77653)
	@Test
	public void test26() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false);
	}

	// extended junit case with generics and deprecation message
	@Test
	public void test27() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false, true);
	}

	// extended junit case with generics, enums and deprecation message
	@Test
	public void test28() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false, true);
	}

	// extended junit case with generics, enums and deprecation message
	@Test
	public void test29() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 9, 20, 9, 23, PARAMETER, "test", false, false, true);
	}

	// extended junit case with generics, enums, static imports and deprecation message
	@Test
	public void test30() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 10, 21, 10, 21, PARAMETER, "test", false, false, true);
	}

	// extended junit case with generics, enums, static imports and deprecation message
	@Ignore("disabled due to missing support for statically imported methods")
	@Test
	public void test31() throws Exception {
		helper1(new String[] { "p1.TR", "p1.TC", "p1.P"}, "p1.TR", 10, 21, 10, 21, PARAMETER, "test", false, false, true);
	}

	@Test
	public void test32() throws Exception {
		helper1(new String[] { "p1.A"}, "p1.A", 9, 25, 9, 26, PARAMETER, "p", true, true);
	}

	// Test visibility of a target field is not affected.
	@Test
	public void test33() throws Exception {
		helper1(new String[] { "p.Foo", "p.Bar" }, "p.Foo", 6, 18, 6, 21, FIELD, "_bar", false, false);
	}

	// Test visibility of target field is changed to public
	// in case a caller is in another package (bug 117465).
	@Test
	public void test34() throws Exception {
		helper1(new String[] { "test1.TestTarget", "test1.Test1", "test2.Test2"}, "test1.Test1", 3, 21, 3, 33, FIELD, "target", true, true);
	}

	// Test visibility of target field is changed to default
	// in case a caller is in the same package (bug 117465).
	@Test
	public void test35() throws Exception {
		helper1(new String[] { "test1.TestTarget", "test1.Test1", "test1.Test2"}, "test1.Test1", 3, 21, 3, 33, FIELD, "target", true, true);
	}

	// Test search engine for secondary types (bug 108030).
	@Test
	public void test36() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 9, 17, 9, 27, FIELD, "fB", false, false);
	}

	// Test name conflicts in the moved method between fields and parameters (bug 227876)
	@Test
	public void test37() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 4, 17, 4, 42, FIELD, "destination", true, true);
	}

	// Test problem with parameter order (bug 165697)
	@Test
	public void test38() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 4, 17, 4, 35, FIELD, "target", true, true);
	}

	// Test problem with qualified accesses (bug 149316)
	@Test
	public void test39() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 4, 13, 4, 25, PARAMETER, "p", true, true);
	}

	// Test problem with qualified accesses (bug 149316)
	@Test
	public void test40() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 4, 13, 4, 25, PARAMETER, "p", true, true);
	}

	// Test problem with missing bindings (bug 328554)
	@Test
	public void test41() throws Exception {
		helper1(new String[] { "p.A" }, "p.A", 4, 10, 4, 10, PARAMETER, "b", true, true);
	}

	// Test problem with parameterized nested class (bug 342074)
	@Test
	public void test42() throws Exception {
		helper1(new String[] { "p.A", "p.B", "p.Outer" }, "p.A", 6, 17, 6, 20, PARAMETER, "b", true, true);
	}

	// Test problem with enum (bug 339980)
	@Test
	public void test43() throws Exception {
		helper1(new String[] { "p.A" }, "p.A", 10, 10, 10, 20, PARAMETER, "fooBar", true, true);
	}

	// Test problem with enum (bug 339980)
	@Test
	public void test44() throws Exception {
		helper1(new String[] { "p.A", "p.MyEnum" }, "p.A", 8, 10, 8, 20, PARAMETER, "fooBar", true, true);
	}

	// Test problem with enum (bug 339980)
	@Test
	public void test45() throws Exception {
		helper1(new String[] { "p.A" }, "p.A", 8, 10, 8, 20, PARAMETER, "fooBar", true, true);
	}

	// bug 385989
	@Test
	public void test46() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 6, 10, 6, 13, PARAMETER, "b", true, true);
	}

	// bug 385550
	@Test
	public void test47() throws Exception {
		helper1(new String[] { "p.A" }, "p.A", 8, 17, 8, 17, PARAMETER, "target", true, true);
	}

	// bug 411529
	@Test
	public void test48() throws Exception {
		helper1(new String[] { "p.A", "p.B", "q.C" }, "p.B", 3, 17, 3, 17, PARAMETER, "c", true, true);
	}

	//bug 356687
	@Test
	public void test49() throws Exception {
		helper1(new String[] {"p.A"}, "p.A", 5, 10, 5, 11, FIELD, "b", true, true);
	}

	//bug 356687
	@Test
	public void test50() throws Exception {
		helper1(new String[] {"p.A"}, "p.A", 4, 10, 4, 11, PARAMETER, "b", true, true);
	}

	//bug 356687
	@Test
	public void test51() throws Exception {
		helper1(new String[] {"p.A"}, "p.A", 4, 10, 4, 11, PARAMETER, "b", true, true);
	}

	//bug 356687
	@Test
	public void test52() throws Exception {
		helper1(new String[] {"p.A"}, "p.A", 5, 10, 5, 11, FIELD, "b", true, true);
	}

	//bug 356687
	@Test
	public void test53() throws Exception {
		helper1(new String[] {"p.A"}, "p.A", 4, 10, 4, 11, PARAMETER, "b", true, true);
	}

	//bug 356687
	@Test
	public void test54() throws Exception {
		helper1(new String[] {"p.A"}, "p.A", 4, 15, 4, 16, PARAMETER, "b", false, false);
	}

	//bug 356687
	@Test
	public void test55() throws Exception {
		helper1(new String[] {"p.A"}, "p.A", 4, 17, 4, 18, FIELD, "b", true, true);
	}

	//bug 356687
	@Test
	public void test56() throws Exception {
		helper1(new String[] {"p.A"}, "p.A", 3, 17, 3, 18, PARAMETER, "b", true, true);
	}

	//bug 356687
	@Test
	public void test57() throws Exception {
		helper1(new String[] { "p.A" }, "p.A", 5, 17, 5, 18, PARAMETER, "b", true, true);
	}

	//bug 217753
	@Test
	public void test58() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 8, 25, 8, 28, PARAMETER, "b", true, true);
	}

	// bug 217753
	@Test
	public void test59() throws Exception {
		helper1(new String[] { "p.A", "p.B" }, "p.A", 5, 14, 5, 15, PARAMETER, "b", true, true);
	}

	// bug 217753
	@Test
	public void test60() throws Exception {
		helper1(new String[] { "p.A" }, "p.A", 5, 14, 5, 15, PARAMETER, "b", true, true);
	}

	// bug 217753
	@Test
	public void test61() throws Exception {
		helper1(new String[] { "p.A" }, "p.A", 5, 14, 5, 15, PARAMETER, "b", true, true);
	}

	// bug 217753
	@Test
	public void test62() throws Exception {
		helper1(new String[] { "p.A", "q.B" }, "p.A", 8, 14, 8, 15, PARAMETER, "c", true, true);
	}

	// bug 217753
	@Test
	public void test63() throws Exception {
		helper1(new String[] { "A" }, "A", 2, 10, 2, 11, PARAMETER, "b", true, true);
	}

	// bug 404477
	@Test
	public void test64() throws Exception {
		helper1(new String[] { "A" }, "A", 3, 17, 3, 18, PARAMETER, "b", true, true);
	}

	// bug 404471
	@Test
	public void test65() throws Exception {
		helper1(new String[] { "A" }, "A", 3, 17, 3, 18, PARAMETER, "c", false, false);
	}

	// bug 426112
	@Test
	public void test66() throws Exception {
		helper1(new String[] { "A" }, "A", 2, 17, 2, 20, PARAMETER, "a", true, true);
	}

	// bug 436997 - references enclosing generic type
	@Test
	public void test67() throws Exception {
		helper1(new String[] { "A" }, "A", 6, 14, 6, 15, FIELD, "b", true, true);
	}

	// bug 441217
	@Test
	public void test68() throws Exception {
		helper1(new String[] { "A" }, "A", 6, 16, 6, 17, PARAMETER, "d", true, true);
	}

	// bug 486175
	@Test
	public void test69() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 11, 17, 11, 23, FIELD, "b", true, true);
	}

	// Issue 1300
	@Test
	public void test70() throws Exception {
		helper1(new String[] { "A" }, "A", 10, 17, 10, 18, FIELD, "b", true, true);
	}

	// Issue 1301
	@Test
	public void test71() throws Exception {
		helper1(new String[] { "p1.A" }, "p1.A", 13, 17, 13, 18, FIELD, "b", true, true);
	}

	// Issue 1301
	@Test
	public void test72() throws Exception {
		helper1(new String[] { "p1.A" }, "p1.A", 13, 17, 13, 18, FIELD, "b", true, true);
	}

	// Issue 1301
	@Test
	public void test73() throws Exception {
		helper1(new String[] { "p1.A", "p1.B" }, "p1.A", 13, 17, 13, 18, FIELD, "b", true, true);
	}

	// Issue 1301
	@Test
	public void test74() throws Exception {
		helper1(new String[] { "p1.A", "p1.B" }, "p1.A", 13, 17, 13, 18, FIELD, "b", true, true);
	}

	// Issue 1303
	@Test
	public void test75() throws Exception {
		helper1(new String[] { "p1.A" }, "p1.A", 13, 17, 13, 18, FIELD, "b", true, true);
	}

	// Issue 1303
	@Test
	public void test76() throws Exception {
		helper1(new String[] { "p1.A", "p1.B" }, "p1.A", 13, 17, 13, 18, FIELD, "b", true, true);
	}

	// Issue 1304
	@Test
	public void test77() throws Exception {
		helper1(new String[] { "p1.A" }, "p1.A", 12, 17, 12, 18, FIELD, "b", true, true);
	}

	// Issue 1304
	@Test
	public void test78() throws Exception {
		helper1(new String[] { "p1.A", "p1.B" }, "p1.A", 12, 17, 12, 18, FIELD, "b", true, true);
	}

	@Test
	public void test79() throws Exception {
		helper1(new String[] { "A" }, "A", 11, 17, 11, 18, FIELD, "b", true, true);
	}
	// Move mA1 to field fB, do not inline delegator
	@Test
	public void test3() throws Exception {
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 9, 17, 9, 20, FIELD, "fB", false, false);
	}

	// // Move mA1 to field fB, inline delegator, remove delegator
	@Test
	public void test4() throws Exception {
		// printTestDisabledMessage("not implemented yet");
		helper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 9, 17, 9, 20, FIELD, "fB", true, true);
	}

	// Move mA1 to field fB, unqualified static member references are qualified
	@Test
	public void test5() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 15, 19, 15, 19, FIELD, "fB", false, false);
	}

	// class qualify referenced type name to top level, original receiver not used in method
	@Test
	public void test6() throws Exception {
		helper1(new String[] { "p1.Nestor", "p2.B"}, "p1.Nestor", 11, 17, 11, 17, PARAMETER, "b", false, false);
	}

	@Test
	public void test7() throws Exception {
		helper1(new String[] { "p1.A", "p2.B", "p3.N1"}, "p1.A", 8, 17, 8, 18, PARAMETER, "b", false, false);
	}

	// access to fields, non-void return type
	@Test
	public void test8() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 15, 19, 15, 20, PARAMETER, "b", false, false);
	}

	// multiple parameters, some left of new receiver parameter, some right of it,
	// "this" is passed as argument
	@Test
	public void test9() throws Exception {
		helper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 17, 6, 17, PARAMETER, "b", false, false);
	}

	// Cannot move interface method declaration
	@Test
	public void testFail0() throws Exception {
		failHelper1("p1.IA", 5, 17, 5, 20, PARAMETER, "b", true, true);
	}

	// Cannot move abstract method declaration
	@Test
	public void testFail1() throws Exception {
		failHelper1("p1.A", 5, 26, 5, 29, PARAMETER, "b", true, true);
	}

	// Cannot move method if there's no new potential receiver
	@Test
	public void testFail10() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 8, 17, 8, 20, PARAMETER, "b", true, true);
	}

	// Cannot move method - parameter name conflict
	@Test
	public void testFail11() throws Exception {
		failHelper2(new String[] { "p1.A", "p2.B"}, "p1.A", 7, 17, 7, 20, PARAMETER, "b", "a", true, true);
	}

	// Cannot move method if there's no new potential receiver (because of null bindings here)
	@Test
	public void testFail12() throws Exception {
		// printTestDisabledMessage("bug 39871");
		failHelper1(new String[] { "p1.A"}, "p1.A", 5, 10, 5, 16, PARAMETER, "b", true, true);
	}

	// Cannot move method - annotations are not supported
	@Ignore("disabled - jcore does not have elements for annotation members")
	@Test
	public void testFail13() throws Exception {
		failHelper2(new String[] { "p1.A", "p2.B"}, "p1.A", 5, 12, 5, 13, PARAMETER, "b", "a", true, true);
	}

	// bug 404477 - target method already exists
	@Test
	public void testFail14() throws Exception {
		failHelper1(new String[] { "A" }, "A", 2, 17, 2, 18, PARAMETER, "b", true, true);
	}

	// bug 404477 / bug 286221 - target method already exists
	@Test
	public void testFail15() throws Exception {
		failHelper1(new String[] { "A" }, "A", 3, 17, 3, 18, FIELD, "fB", true, true);
	}

	// bug 436997 - references enclosing instance
	@Test
	public void testFail16() throws Exception {
		failHelper1(new String[] { "p1.A", "p1.B"}, "p1.A", 8, 14, 8, 15, PARAMETER, "b", true, true);
	}

	// Issue 1404
	@Test
	public void testFail17() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 13, 16, 13, 24, FIELD, "a1", true, true);
	}

	// Cannot move static method
	@Test
	public void testFail2() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 23, 6, 24, PARAMETER, "b", true, true);
	}

	// Cannot move native method
	@Test
	public void testFail3() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 23, 6, 24, PARAMETER, "b", true, true);
	}

	// Cannot move method that references "super"
	@Test
	public void testFail4() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 11, 20, 11, 21, PARAMETER, "b", true, true);
	}

	// Cannot move method that references an enclosing instance
	@Test
	public void testFail5() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 8, 21, 8, 21, PARAMETER, "b", true, true);
	}

	// Cannot move potentially directly recursive method
	@Test
	public void testFail6() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 16, 6, 17, PARAMETER, "b", true, true);
	}

	// Cannot move synchronized method
	@Test
	public void testFail8() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B"}, "p1.A", 6, 29, 6, 29, PARAMETER, "b", true, true);
	}

	// Cannot move method if there's no new potential receiver
	@Test
	public void testFail9() throws Exception {
		failHelper1(new String[] { "p1.A", "p2.B", "p3.C"}, "p1.A", 7, 17, 7, 20, PARAMETER, "b", true, true);
	}
}
