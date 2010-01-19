/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.AST;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

public class InlineConstantTests extends RefactoringTest {
	private static final Class clazz = InlineConstantTests.class;
	private static final String REFACTORING_PATH = "InlineConstant/";

	private boolean toSucceed;

	public InlineConstantTests(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH + successPath();
	}

	private String successPath() {
		return toSucceed ? "/canInline/" : "/cannotInline/";
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test test) {
		return new RefactoringTestSetup(test);
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
	private void helper1(String cuQName, int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean removeDeclaration) throws Exception{
		helper1(new String[] {cuQName}, cuQName, startLine, startColumn, endLine, endColumn, replaceAll, removeDeclaration);
	}
	private void helper1(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean removeDeclaration) throws Exception{
		int selectionCuIndex= firstIndexOf(selectionCuQName, cuQNames);
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		helper1(cuQNames, selectionCuIndex, startLine, startColumn, endLine, endColumn, replaceAll, removeDeclaration);
	}
	private void helper1(String[] cuQNames, int selectionCuIndex, int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean removeDeclaration) throws Exception{
		Assert.isTrue(0 <= selectionCuIndex && selectionCuIndex < cuQNames.length);

		toSucceed= true;

		ICompilationUnit[] cus= createCUs(cuQNames);
		ICompilationUnit selectionCu= cus[selectionCuIndex];

		ISourceRange selection= TextRangeUtil.getSelection(selectionCu, startLine, startColumn, endLine, endColumn);
		InlineConstantRefactoring ref= new InlineConstantRefactoring(selectionCu, new RefactoringASTParser(AST.JLS3).parse(selectionCu, true), selection.getOffset(), selection.getLength());
		if (ref.checkStaticFinalConstantNameSelected().hasFatalError())
			ref= null;
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", preconditionResult.isOK());

		ref.setReplaceAllReferences(replaceAll);
		ref.setRemoveDeclaration(removeDeclaration);

		preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));

		assertTrue("precondition was supposed to pass",preconditionResult.isOK());

		performChange(ref, false);

		for(int i= 0; i < cus.length; i++){
			String outputTestFileName= getOutputTestFileName(getSimpleName(cuQNames[i]));
			assertEqualLines("Incorrect inline in " + outputTestFileName, getFileContents(outputTestFileName), cus[i].getSource());
		}
	}

	private void failHelper1(String cuQName, int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean removeDeclaration, int errorCode) throws Exception{
		failHelper1(new String[] {cuQName}, cuQName, startLine, startColumn, endLine, endColumn, replaceAll, removeDeclaration, errorCode);
	}
	private void failHelper1(String[] cuQNames, String selectionCuQName, int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean removeDeclaration, int errorCode) throws Exception{
		int selectionCuIndex= firstIndexOf(selectionCuQName, cuQNames);
		Assert.isTrue(selectionCuIndex != -1, "parameter selectionCuQName must match some String in cuQNames.");
		failHelper1(cuQNames, selectionCuIndex, startLine, startColumn, endLine, endColumn, replaceAll, removeDeclaration, errorCode);
	}
	private void failHelper1(String[] cuQNames, int selectionCuIndex, int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean removeDeclaration, int errorCode) throws Exception{
		Assert.isTrue(0 <= selectionCuIndex && selectionCuIndex < cuQNames.length);

		toSucceed= false;

		ICompilationUnit[] cus= createCUs(cuQNames);
		ICompilationUnit selectionCu= cus[selectionCuIndex];

		ISourceRange selection= TextRangeUtil.getSelection(selectionCu, startLine, startColumn, endLine, endColumn);
		InlineConstantRefactoring ref= new InlineConstantRefactoring(selectionCu, new RefactoringASTParser(AST.JLS3).parse(selectionCu, true), selection.getOffset(), selection.getLength());
		if (ref.checkStaticFinalConstantNameSelected().hasFatalError())
			ref= null;
		if (ref == null)
			return;
		RefactoringStatus result= ref.checkInitialConditions(new NullProgressMonitor());

		if(!result.isOK()) {
			assertEquals(errorCode, result.getEntryMatchingSeverity(RefactoringStatus.ERROR).getCode());
			return;
		} else {

			ref.setReplaceAllReferences(replaceAll);
			ref.setRemoveDeclaration(removeDeclaration);

			result.merge(ref.checkFinalConditions(new NullProgressMonitor()));

			assertTrue("precondition checking is expected to fail.", !result.isOK());
			assertEquals(errorCode, result.getEntryMatchingSeverity(RefactoringStatus.ERROR).getCode());
		}
	}

	//--- TESTS

	public void test0() throws Exception {
		helper1("p.C", 5, 30, 5, 36, true, false);
	}

	public void test1() throws Exception {
		helper1("C", 3, 33, 3, 40, true, false);
	}

	public void test2() throws Exception {
		helper1("p.Klass", 10, 22, 10, 30, false, false);
	}

	public void test3() throws Exception {
		helper1("p.LeVinSuperieure", 5, 32, 5, 43, true, true);
	}

	public void test4() throws Exception {
		helper1("p.Klus", 5, 36, 5, 36, true, false);
	}

	public void test5() throws Exception {
		helper1("p.PartOfDeclNameSelected", 5, 32, 5, 34, true, true);
	}

	public void test6() throws Exception {
		helper1("p.CursorPositionedInReference", 8, 57, 8, 57, false, false);
	}

	public void test7() throws Exception {
		helper1("p.PartOfReferenceSelected", 8, 52, 8, 62, false, false);
	}

	public void test8() throws Exception {
		helper1(new String[] {"p1.C", "p2.D"}, "p1.C", 5, 29, 5, 37, true, false);
	}

	public void test9() throws Exception {
		helper1(new String[] {"p1.C", "p2.D", "p3.E"}, "p2.D", 8, 18, 8, 26, true, true);
	}

	public void test10() throws Exception {
		helper1(new String[] {"p1.A", "p2.B"}, "p2.B", 9, 28, 9, 37, false, false);
	}

	public void test11() throws Exception {
		helper1(new String[] {"p1.A", "p2.B", "p3.C"}, "p1.A", 8, 25, 8, 25, false, false);
	}

	public void test12() throws Exception {
		helper1(new String[] {"p1.Declarer", "p2.InlineSite"}, "p2.InlineSite", 7, 37, 7, 43, true, false);
	}

	public void test13() throws Exception {
		helper1(new String[] {"p1.A", "p2.InlineSite"}, "p2.InlineSite", 8, 19, 8, 29, false, false);
	}

	public void test14() throws Exception {
		helper1("cantonzuerich.GrueziWohl", 7, 35, 7, 35, true, false);
	}

	public void test15() throws Exception {
		helper1("schweiz.zuerich.zuerich.Froehlichkeit", 14, 16, 14, 32, true, false);
	}

	public void test16() throws Exception {
		helper1("p.IntegerMath", 8, 23, 8, 23, true, true);
	}

	public void test17() throws Exception {
		helper1("p.EnumRef", 4, 59, 4, 59, true, true);
	}

	public void test18() throws Exception {
		helper1("p.Annot", 5, 18, 5, 18, true, true);
	}

	public void test19() throws Exception {
		helper1("p.Test", 7, 36, 7, 36, true, false);
	}

	public void test20() throws Exception {
		helper1("p.Test", 10, 21, 10, 21, true, true);
	}

	public void test21() throws Exception {
		helper1(new String[] {"p.A", "q.Consts"}, "p.A", 8, 16, 8, 19, true, false);
	}

	public void test22() throws Exception {
		helper1(new String[] {"p.A", "q.Consts", "r.Third"}, "p.A", 11, 16, 11, 19, true, true);
	}

	public void test23() throws Exception {
		helper1("p.Test", 6, 26, 6, 26, false, false);
	}

	public void test24() throws Exception {
		helper1(new String[] {"p.A", "q.Consts"}, "p.A", 14, 17, 14, 17, true, true);
	}

	public void test25() throws Exception {
		helper1("p.A", 5, 32, 5, 32, true, true);
	}

	public void test26() throws Exception { // test for bug 93689
		helper1("p.A", 5, 42, 5, 42, true, true);
	}

	public void test27() throws Exception { // test for bug 109071
		helper1("p.A", 4, 24, 4, 29, true, true);
	}

	public void test28() throws Exception {
		helper1(new String[] {"p.Const", "p.AnotherClass", "q.UsedClass"}, "p.Const", 6, 35, 6, 43, true, true);
	}

	public void test29() throws Exception { // test for bug 174327
		helper1("p.A", 7, 44, 7, 44, true, true);
	}

	public void test30() throws Exception { //test for bug 237547 (inline unused constant)
		helper1(new String[] {"p.A", "p.B", "p.C", "p.D", "q.Consts"}, "q.Consts", 5, 32, 5, 40, true, true);
	}

	public void test31() throws Exception { // test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=265448
		helper1("p.A", 4, 23, 4, 28, true, true);
	}
	
	public void test32() throws Exception { // test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=265448
		helper1("p.A", 4, 23, 4, 28, true, true);
	}
	
	public void test33() throws Exception { // test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=279715
		helper1("p.A", 5, 29, 5, 30, true, true);
	}
	
	public void test34() throws Exception { // test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=297760
		helper1("p.A", 4, 24, 4, 25, true, true);
	}
	
	// -- testing failing preconditions

	public void testFail0() throws Exception {
		failHelper1("foo.NeueZuercherZeitung", 5, 27, 5, 28, true, false, RefactoringStatusCodes.NOT_STATIC_FINAL_SELECTED);
	}

	public void testFail1() throws Exception {
		failHelper1("fun.Fun", 8, 35, 8, 35, false, false, RefactoringStatusCodes.DECLARED_IN_CLASSFILE);
	}

	public void testFail2() throws Exception {
		failHelper1("p.EnumRef", 7, 22, 7, 22, true, true, RefactoringStatusCodes.NOT_STATIC_FINAL_SELECTED);
	}
}
