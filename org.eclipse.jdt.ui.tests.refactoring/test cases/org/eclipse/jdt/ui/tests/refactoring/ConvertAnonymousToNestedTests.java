/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     N.Metchev@teamphone.com - contributed fixes for
 *     - convert anonymous to nested should sometimes declare class as static [refactoring]
 *       (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=43360)
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.code.ConvertAnonymousToNestedRefactoring;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

public class ConvertAnonymousToNestedTests extends RefactoringTest {

	private static final Class clazz= ConvertAnonymousToNestedTests.class;
	private static final String REFACTORING_PATH= "ConvertAnonymousToNested/";

	private Object fCompactPref;

	public ConvertAnonymousToNestedTests(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	public static Test suite() {
		return new Java15Setup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
	    return new RefactoringTestSetup(someTest);
	}

	private String getSimpleTestFileName(boolean canInline, boolean input){
		String fileName = "A_" + getName();
		if (canInline)
			fileName += input ? "_in": "_out";
		return fileName + ".java";
	}

	private String getTestFileName(boolean canConvert, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canConvert ? "canConvert/": "cannotConvert/");
		return fileName + getSimpleTestFileName(canConvert, input);
	}

	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canConvert, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canConvert, input), getFileContents(getTestFileName(canConvert, input)));
	}

	protected void setUp() throws Exception {
		super.setUp();
		Hashtable options= JavaCore.getOptions();

		String setting= DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR;
		fCompactPref= options.get(setting);
		options.put(setting, DefaultCodeFormatterConstants.TRUE);
		JavaCore.setOptions(options);

		IJavaProject project= getPackageP().getJavaProject();
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "", project);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, "", project);
		StubUtility.setCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, "", project);

	}

	protected void tearDown() throws Exception {
		super.tearDown();
		Hashtable options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, fCompactPref);
		JavaCore.setOptions(options);
	}

	private void helper1(int startLine, int startColumn, int endLine, int endColumn, boolean makeFinal, String className, int visibility) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ConvertAnonymousToNestedRefactoring ref= new ConvertAnonymousToNestedRefactoring(cu, selection.getOffset(), selection.getLength());

		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());
		if (preconditionResult.isOK())
			preconditionResult= null;
		assertEquals("activation was supposed to be successful", null, preconditionResult);

		ref.setClassName(className);
		ref.setDeclareFinal(makeFinal);
		ref.setVisibility(visibility);

		if (preconditionResult == null)
			preconditionResult= ref.checkFinalConditions(new NullProgressMonitor());
		else
			preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));
		if (preconditionResult.isOK())
			preconditionResult= null;
		assertEquals("precondition was supposed to pass", null, preconditionResult);

		performChange(ref, false);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEqualLines(getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void helper1(int startLine, int startColumn, int endLine, int endColumn, boolean makeFinal, boolean makeStatic, String className, int visibility) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ConvertAnonymousToNestedRefactoring ref= new ConvertAnonymousToNestedRefactoring(cu, selection.getOffset(), selection.getLength());

		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());
		if (preconditionResult.isOK())
			preconditionResult= null;
		assertEquals("activation was supposed to be successful", null, preconditionResult);

		ref.setClassName(className);
		ref.setDeclareFinal(makeFinal);
		ref.setDeclareStatic(makeStatic);
		ref.setVisibility(visibility);

		if (preconditionResult == null)
			preconditionResult= ref.checkFinalConditions(new NullProgressMonitor());
		else
			preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));
		if (preconditionResult.isOK())
			preconditionResult= null;
		assertEquals("precondition was supposed to pass", null, preconditionResult);

		performChange(ref, false);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEqualLines(getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void failHelper1(int startLine, int startColumn, int endLine, int endColumn, boolean makeFinal, String className, int visibility, int expectedSeverity) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ConvertAnonymousToNestedRefactoring ref= new ConvertAnonymousToNestedRefactoring(cu, selection.getOffset(), selection.getLength());

		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());
		if (preconditionResult.isOK())
			preconditionResult= null;
		assertEquals("activation was supposed to be successful", null, preconditionResult);

		ref.setClassName(className);
		ref.setDeclareFinal(makeFinal);
		ref.setVisibility(visibility);

		if (preconditionResult == null)
			preconditionResult= ref.checkFinalConditions(new NullProgressMonitor());
		else
			preconditionResult.merge(ref.checkFinalConditions(new NullProgressMonitor()));
		if (preconditionResult.isOK())
			preconditionResult= null;
		assertNotNull("precondition was supposed to fail",preconditionResult);

		assertEquals("incorrect severity:", expectedSeverity, preconditionResult.getSeverity());
	}

	private void failActivationHelper(int startLine, int startColumn, int endLine, int endColumn, int expectedSeverity) throws Exception {
	    ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
	    ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
	    ConvertAnonymousToNestedRefactoring ref= new ConvertAnonymousToNestedRefactoring(cu, selection.getOffset(), selection.getLength());

	    RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());
	    assertEquals("activation was supposed to fail", expectedSeverity, preconditionResult.getSeverity());
	}

	//--- TESTS

	public void testFail0() throws Exception{
		printTestDisabledMessage("corner case - local types");
//		failHelper1(6, 14, 6, 16, true, "Inner", Modifier.PRIVATE, RefactoringStatus.FATAL);
	}

	public void testFail1() throws Exception{
		failHelper1(5, 17, 5, 17, true, "Inner", Modifier.PRIVATE, RefactoringStatus.FATAL);
	}

	public void testFail2() throws Exception{
		failHelper1(5, 17, 5, 18, true, "Inner", Modifier.PRIVATE, RefactoringStatus.FATAL);
	}

	public void testFail3() throws Exception{
		failActivationHelper(13, 27, 13, 27, RefactoringStatus.FATAL);
	}

	public void testFail4() throws Exception{
	    failHelper1(8, 31, 8, 31, true, "Inner", Modifier.PRIVATE, RefactoringStatus.ERROR);
	}

	public void test0() throws Exception{
		helper1(5, 17, 5, 17, true, "Inner", Modifier.PRIVATE);
	}

	public void test1() throws Exception{
		helper1(5, 17, 5, 17, true, "Inner", Modifier.PUBLIC);
	}

	public void test2() throws Exception{
		helper1(5, 17, 5, 17, true, "Inner", Modifier.PUBLIC);
	}

	public void test3() throws Exception{
		helper1(5, 17, 5, 17, false, "Inner", Modifier.PUBLIC);
	}

	public void test4() throws Exception{
		helper1(7, 17, 7, 17, true, "Inner", Modifier.PRIVATE);
	}

	public void test5() throws Exception{
		helper1(7, 17, 7, 19, true, "Inner", Modifier.PRIVATE);
	}

	public void test6() throws Exception{
		helper1(8, 13, 9, 14, true, "Inner", Modifier.PRIVATE);
	}

	public void test7() throws Exception{
		helper1(7, 18, 7, 18, true, "Inner", Modifier.PRIVATE);
	}

	public void test8() throws Exception{
		helper1(8, 14, 8, 15, true, "Inner", Modifier.PRIVATE);
	}

	public void test9() throws Exception{
		helper1(8, 13, 8, 14, true, "Inner", Modifier.PRIVATE);
	}

	public void test10() throws Exception{
		helper1(7, 13, 7, 14, true, "Inner", Modifier.PRIVATE);
	}

	public void test11() throws Exception{
		helper1(5, 15, 5, 17, true, "Inner", Modifier.PRIVATE);
	}

	public void test12() throws Exception{
		helper1(8, 9, 10, 10, true, "Inner", Modifier.PRIVATE);
	}

	public void test13() throws Exception{
		helper1(6, 28, 6, 28, true, "Inner", Modifier.PRIVATE);
	}

	public void test14() throws Exception{
		helper1(5, 13, 5, 23, true, "Inner", Modifier.PRIVATE);
	}

	public void test15() throws Exception{
		helper1(7, 26, 7, 26, true, "Inner", Modifier.PRIVATE);
	}

	public void test16() throws Exception{
		helper1(4, 10, 4, 26, true, "Inner", Modifier.PRIVATE);
	}

	public void test17() throws Exception{
		helper1(6, 14, 6, 15, true, "Inner", Modifier.PRIVATE);
	}

	public void test18() throws Exception{
		helper1(5, 15, 5, 17, true, "Inner", Modifier.PRIVATE);
	}

	public void test19() throws Exception{
		helper1(5, 12, 6, 21, true, "Inner", Modifier.PRIVATE);
	}

	public void test20() throws Exception{
		helper1(4, 25, 4, 25, true, "Inner", Modifier.PRIVATE);
	}

    public void test21() throws Exception{
        helper1(4, 25, 4, 25, true, "Inner", Modifier.PRIVATE);
    }

    public void test22() throws Exception{
    	helper1(9, 34, 9, 34, true, "Inner", Modifier.PRIVATE);
    }

    public void test23() throws Exception{
    	helper1(6, 33, 6, 33, true, "Inner", Modifier.PRIVATE);
    }

    public void test24() throws Exception{
    	helper1(3, 26, 3, 26, true, "Inner", Modifier.PRIVATE);
    }

    public void test25() throws Exception{
    	helper1(8, 28, 8, 28, true, "Inner", Modifier.PRIVATE);
    }

    public void test26() throws Exception{
    	helper1(8, 28, 8, 28, true, "Inner", Modifier.PRIVATE);
    }

    public void test27() throws Exception{
    	helper1(11, 39, 11, 39, true, "Inner", Modifier.PRIVATE);
    }

    public void test28() throws Exception{
//        printTestDisabledMessage("disabled: bug 43360");
    	helper1(10, 27, 10, 27, true, "Inner", Modifier.PRIVATE);
    }

    public void test29() throws Exception{
    		helper1(6, 14, 6, 14, true, "Inner", Modifier.PRIVATE);
    }

    public void test30() throws Exception{ // 2 syntax errors
    	helper1(5, 32, 5, 32, true, true, "Greeter", Modifier.PRIVATE);
    }

	public void testGenerics0() throws Exception{
		helper1(5, 20, 5, 20, true, "Inner", Modifier.PRIVATE);
	}

	public void testGenerics1() throws Exception{
		helper1(5, 20, 5, 20, true, "Inner", Modifier.PUBLIC);
	}

	public void testGenerics2() throws Exception{
		helper1(5, 20, 5, 20, true, "Inner", Modifier.PUBLIC);
	}

	public void testGenerics3() throws Exception{
		helper1(5, 20, 5, 20, false, "Inner", Modifier.PUBLIC);
	}

	public void testGenerics4() throws Exception{
		helper1(7, 20, 7, 20, true, "Inner", Modifier.PRIVATE);
	}

	public void testGenerics5() throws Exception{
		helper1(7, 20, 7, 20, true, "Inner", Modifier.PRIVATE);
	}

	public void testGenerics6() throws Exception{
		helper1(7, 20, 7, 20, true, true, "Inner", Modifier.PRIVATE);
	}

    public void test31() throws Exception{ // for bug 181054
    	helper1(10, 24, 10, 30, true, false, "Inner1Extension", Modifier.PRIVATE);
    }

    public void test32() throws Exception{ // for bug 158028
    	helper1(10, 30, 10, 36, true, false, "Inner1Extension", Modifier.PRIVATE);
    }
}
