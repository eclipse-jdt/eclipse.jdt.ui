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
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;

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
		InlineConstantRefactoring ref= InlineConstantRefactoring.create(selectionCu, selection.getOffset(), selection.getLength(), JavaPreferencesSettings.getCodeGenerationSettings());

		RefactoringStatus preconditionResult= ref.checkActivation(new NullProgressMonitor());	

		assertTrue("activation was supposed to be successful", preconditionResult.isOK());

		ref.setReplaceAllReferences(replaceAll);
		ref.setRemoveDeclaration(removeDeclaration);
		
		preconditionResult.merge(ref.checkInput(new NullProgressMonitor()));

		assertTrue("precondition was supposed to pass",preconditionResult.isOK());

		IChange change= ref.createChange(new NullProgressMonitor());
		performChange(change);

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
		InlineConstantRefactoring ref= InlineConstantRefactoring.create(selectionCu, selection.getOffset(), selection.getLength(), 
										
		JavaPreferencesSettings.getCodeGenerationSettings());
		if (ref == null)
			return;
		RefactoringStatus result= ref.checkActivation(new NullProgressMonitor());	

		if(!result.isOK()) {
			assertEquals(errorCode, result.getEntryMatchingSeverity(RefactoringStatus.ERROR).getCode());
			return;				
		} else {

			ref.setReplaceAllReferences(replaceAll);
			ref.setRemoveDeclaration(removeDeclaration);
			
			result.merge(ref.checkInput(new NullProgressMonitor()));
	
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
		
	// -- testing failing preconditions
	
	public void testFail0() throws Exception {
		failHelper1("foo.NeueZuercherZeitung", 5, 27, 5, 28, true, false, RefactoringStatusCodes.NOT_STATIC_FINAL_SELECTED);
	}
	
	public void testFail1() throws Exception {
		failHelper1("fun.Fun", 8, 35, 8, 35, false, false, RefactoringStatusCodes.DECLARED_IN_CLASSFILE);	
	}
}