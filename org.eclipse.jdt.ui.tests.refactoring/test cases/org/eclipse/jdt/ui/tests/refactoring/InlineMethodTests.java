/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fixes for:
 *       o bug "inline method - doesn't handle implicit cast" (see
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *       o bug inline method: compile error (array related) [refactoring]
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38471)
 * 	     o bug "Inline refactoring showed bogus error" (see bugzilla
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=42753)
 *       o inline call that is used in a field initializer
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38137)
 *       o inline call a field initializer: could detect self reference
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=44417)
 *       o Allow 'this' constructor to be inlined
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - Anonymous class using final parameter breaks method inlining - https://bugs.eclipse.org/269401
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.COMPARE_WITH_OUTPUT;
import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.INVALID_SELECTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import org.eclipse.jdt.internal.core.manipulation.dom.OperatorPrecedence;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

public class InlineMethodTests extends AbstractJunit4SelectionTestCase {
	private static final boolean BUG_82166= true;

	public InlineMethodTests() {
		super(true);
	}

	@Rule
	public InlineMethodTestSetup fgTestSetup= new InlineMethodTestSetup();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		fIsPreDeltaTest= true;
	}

	@Override
	protected String getResourceLocation() {
		return "InlineMethodWorkspace/TestCases/";
	}

	@Override
	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}

	protected void performTestInlineCall(IPackageFragment packageFragment, String id, TestMode mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		int[] selection= getSelection();
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(unit, new RefactoringASTParser(AST.getJLSLatest()).parse(unit, true), selection[0], selection[1]);

		String out= null;
		if (mode == COMPARE_WITH_OUTPUT)
			out= getProofedContent(outputFolder, id);

		performTest(unit, refactoring, mode, out, true);
	}

	private void performTestInlineMethod(IPackageFragment packageFragment, String id, TestMode mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		IType type= unit.getTypes()[0];
		IMethod method= getMethodToInline(type);
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(unit, new RefactoringASTParser(AST.getJLSLatest()).parse(unit, true), method.getNameRange().getOffset(), method.getNameRange().getLength());

		String out= null;
		if (mode == COMPARE_WITH_OUTPUT)
			out= getProofedContent(outputFolder, id);

		performTest(unit, refactoring, mode, out, true);
	}

	private IMethod getMethodToInline(IType type) throws CoreException {
		for (IMethod method : type.getMethods()) {
			if ("toInline".equals(method.getElementName())) {
				return method;
			}
		}
		return null;
	}

	private void performTestInlineFirstConstructor(IPackageFragment packageFragment, String id, TestMode mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		IType type= unit.getTypes()[0];
		IMethod method= getFirstConstructor(type);
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(unit, new RefactoringASTParser(AST.getJLSLatest()).parse(unit, true), method.getNameRange().getOffset(), method.getNameRange().getLength());

		String out= null;
		if (mode == COMPARE_WITH_OUTPUT)
			out= getProofedContent(outputFolder, id);

		performTest(unit, refactoring, mode, out, true);
	}

	private IMethod getFirstConstructor(IType type) throws CoreException {
		for (IMethod method : type.getMethods()) {
			if (method.isConstructor()) {
				return method;
			}
		}
		return null;
	}

	/* *********************** Invalid Tests ******************************* */

	protected void performInvalidTest() throws Exception {
		performTestInlineCall(fgTestSetup.getInvalidPackage(), getName(), INVALID_SELECTION, null);
	}

	private void performInvalidTestInlineMethod() throws Exception {
		performTestInlineMethod(fgTestSetup.getInvalidPackage(), getName(), INVALID_SELECTION, null);
	}

	@Test
	public void testRecursion() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testInvalidFieldInitializer1() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testInvalidFieldInitializer2() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testInvalidFieldInitializer3() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testLocalInitializer() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testInterruptedStatement() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testInterruptedExecutionFlow() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testMultiLocal() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testComplexBody() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testCompileError1() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testCompileError2() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testCompileError3() throws Exception {
		performInvalidTest();
	}

	@Test
	public void testMultipleMethods() throws Exception {
		performInvalidTestInlineMethod();
	}

	@Test
	public void testSuperInThis() throws Exception {
		performInvalidTestInlineMethod();
	}

	@Test
	public void testNotMethodName() throws Exception {
		ICompilationUnit unit= createCU(fgTestSetup.getInvalidPackage(), getName());
		int[] selection= getSelection();
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(unit, new RefactoringASTParser(AST.getJLSLatest()).parse(unit, true), selection[0], selection[1]);
		assertNull(refactoring);
	}

	@Test
	public void test_314407() throws Exception {
		performInvalidTest();
	}

	/* *********************** Simple Tests ******************************* */

	private void performSimpleTest() throws Exception {
		performTestInlineCall(fgTestSetup.getSimplePackage(), getName(), COMPARE_WITH_OUTPUT, "simple_out");
	}

	private void performSimpleTestInlineMethod() throws Exception {
		performTestInlineMethod(fgTestSetup.getSimplePackage(), getName(), COMPARE_WITH_OUTPUT, "simple_out");
	}

	private void performSimpleTestInlineConstrcutor() throws Exception {
		performTestInlineFirstConstructor(fgTestSetup.getSimplePackage(), getName(), COMPARE_WITH_OUTPUT, "simple_out");
	}

	@Test
	public void testBasic1() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testBasic2() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testEmptyBody() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testPrimitiveArray() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testTypeArray() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testInitializer() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testSuper() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testFieldInitializer1() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testFieldInitializer2() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testFieldInitializerAnonymous() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testLabeledStatement() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testConstructor1() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testConstructor2() throws Exception {
		performSimpleTestInlineConstrcutor();
	}

	@Test
	public void testCatchClause() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testTwoCalls() throws Exception {
		performSimpleTestInlineMethod();
	}

	@Test
	public void testNestedCalls() throws Exception {
		performSimpleTestInlineMethod();
	}

	@Test
	public void testSurroundingCallers() throws Exception {
		performSimpleTestInlineMethod();
	}

	@Test
	public void testComment1() throws Exception {
		performSimpleTestInlineMethod();
	}

	@Test
	public void testComment2() throws Exception {
		performSimpleTestInlineMethod();
	}

	/* *********************** Bug Tests ******************************* */

	private void performBugTest() throws Exception {
		performTestInlineCall(fgTestSetup.getBugsPackage(), getName(), COMPARE_WITH_OUTPUT, "bugs_out");
	}

	private void performBugTestInlineMethod() throws Exception {
		performTestInlineMethod(fgTestSetup.getBugsPackage(), getName(), COMPARE_WITH_OUTPUT, "bugs_out");
	}

	@Test
	public void test_72836() throws Exception {
		performBugTest();
	}

	@Test
	public void test_76241() throws Exception {
		performBugTestInlineMethod();
	}

	@Test
	public void test_94426() throws Exception {
		performBugTestInlineMethod();
	}

	@Test
	public void test_95128() throws Exception {
		performBugTestInlineMethod();
	}

	@Test
	public void test_117053() throws Exception {
		performBugTest();
	}

	@Test
	public void test_123356() throws Exception {
		performBugTest();
	}

	@Test
	public void test_44419() throws Exception {
		performBugTest();
	}

	@Test
	public void test_44419_2() throws Exception {
		performBugTest();
	}

	@Test
	public void test_98856() throws Exception {
		performBugTest();
	}

	@Test
	public void test_50139() throws Exception {
		performBugTest();
	}

	@Test
	public void test_287378() throws Exception {
		performBugTest();
	}

	@Test
	public void test_267386() throws Exception {
		performBugTest();
	}

	@Test
	public void test_267386_2() throws Exception {
		performBugTest();
	}

	@Test
	public void test_314407_1() throws Exception {
		performBugTest();
	}

	@Test
	public void test_314407_2() throws Exception {
		performBugTest();
	}

	@Test
	public void test_462038() throws Exception {
		performBugTest();
	}

	/* *********************** Argument Tests ******************************* */

	private void performArgumentTest() throws Exception {
		performTestInlineCall(fgTestSetup.getArgumentPackage(), getName(), COMPARE_WITH_OUTPUT, "argument_out");
	}

	@Test
	public void testFieldReference() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceUnused() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceRead() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceRead2() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceWrite() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceLoop() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceLoop1() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceLoop2() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceLoop3() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceLoop4() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferenceLoop5() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLocalReferencePrefix() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLiteralReferenceRead() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testLiteralReferenceWrite() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testParameterNameUsed1() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testParameterNameUsed2() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testParameterNameUsed3() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testParameterNameUsed4() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testParameterNameUnused1() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testParameterNameUnused2() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testParameterNameUnused3() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testOneRead() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testTwoReads() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testWrite() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testArray() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testVarargs() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testVarargs2() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testVarargs3() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testVarargs4() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testVarargs5() throws Exception {
		performArgumentTest();
	}

	@Test
	public void testVarargs6() throws Exception {
		performArgumentTest();
	}

	@Test
	public void test91470() throws Exception {
		performArgumentTest();
	}

	//see bug 269401
	@Test
	public void testFinalParameter1() throws Exception {
		performArgumentTest();
	}

	//see bug 269401
	@Test
	public void testFinalParameter2() throws Exception {
		performArgumentTest();
	}

	//see bug 269401
	@Test
	public void testFinalParameter3() throws Exception {
		performArgumentTest();
	}

	/* *********************** Name Conflict Tests ******************************* */

	private void performNameConflictTest() throws Exception {
		performTestInlineCall(fgTestSetup.getNameConflictPackage(), getName(), COMPARE_WITH_OUTPUT, "nameconflict_out");
	}

	@Test
	public void testSameLocal() throws Exception {
		performNameConflictTest();
	}

	@Test
	public void testSameType() throws Exception {
		performNameConflictTest();
	}

	@Test
	public void testSameTypeAfter() throws Exception {
		performNameConflictTest();
	}

	@Test
	public void testSameTypeInSibling() throws Exception {
		performNameConflictTest();
	}

	@Test
	public void testLocalInType() throws Exception {
		performNameConflictTest();
	}

	@Test
	public void testFieldInType() throws Exception {
		performNameConflictTest();
	}

	@Test
	public void testSwitchStatement() throws Exception {
		performNameConflictTest();
	}

	@Test
	public void testBlocks() throws Exception {
		performNameConflictTest();
	}

	/* *********************** Call Tests ******************************* */

	private void performCallTest() throws Exception {
		performTestInlineCall(fgTestSetup.getCallPackage(), getName(), COMPARE_WITH_OUTPUT, "call_out");
	}

	@Test
	public void testExpressionStatement() throws Exception {
		performCallTest();
	}

	@Test
	public void testExpressionStatementWithReturn() throws Exception {
		performCallTest();
	}

	@Test
	public void testStatementWithFunction1() throws Exception {
		performCallTest();
	}

	@Test
	public void testStatementWithFunction2() throws Exception {
		performCallTest();
	}

	@Test
	public void testParenthesis() throws Exception {
		performCallTest();
	}

	/* *********************** Expression Tests ******************************* */

	private void performExpressionTest() throws Exception {
		performTestInlineCall(fgTestSetup.getExpressionPackage(), getName(), COMPARE_WITH_OUTPUT, "expression_out");
	}

	@Test
	public void testSimpleExpression() throws Exception {
		performExpressionTest();
	}

	@Test
	public void testSimpleExpressionWithStatements() throws Exception {
		performExpressionTest();
	}

	@Test
	public void testSimpleBody() throws Exception {
		performExpressionTest();
	}

	@Test
	public void testAssignment() throws Exception {
		performExpressionTest();
	}

	@Test
	public void testReturnStatement() throws Exception {
		performExpressionTest();
	}

	@Test
	public void testConditionalExpression() throws Exception {
		performExpressionTest();
	}

	/* *********************** Control Statements Tests ******************************* */

	private void performControlStatementTest() throws Exception {
		performTestInlineCall(fgTestSetup.getControlStatementPackage(), getName(), COMPARE_WITH_OUTPUT, "controlStatement_out");
	}

	@Test
	public void testForEmpty() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testForOne() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testForTwo() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testEnhancedForOne() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testEnhancedForTwo() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testIfThenTwo() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testIfElseTwo() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testForAssignmentOne() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testForAssignmentTwo() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testLabelOne() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testLabelTwo() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testDanglingIf() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testIfWithVariable() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testDanglingIfBug229734() throws Exception {
		performControlStatementTest();
	}

	@Test
	public void testDanglingIfBug229734_2() throws Exception {
		performControlStatementTest();
	}

	/* *********************** Receiver Tests ******************************* */

	private void performReceiverTest() throws Exception {
		performTestInlineCall(fgTestSetup.getReceiverPackage(), getName(), COMPARE_WITH_OUTPUT, "receiver_out");
	}

	private void performReceiverTestInlineMethod() throws Exception {
		performTestInlineMethod(fgTestSetup.getReceiverPackage(), getName(), COMPARE_WITH_OUTPUT, "receiver_out");
	}

	@Test
	public void testNoImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testNameThisReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testNameImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testExpressionZeroImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testExpressionOneImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testExpressionTwoImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testStaticReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testReceiverWithStatic() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testThisExpression() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testFieldReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testExplicitStaticThisFieldReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testExplicitThisFieldReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testExplicitStaticThisMethodReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testExplicitThisMethodReceiver() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testThisReceiver() throws Exception {
		performReceiverTestInlineMethod();
	}

	@Test
	public void testImplicitReceiverMethod() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testImplicitReceiverField() throws Exception {
		performReceiverTest();
	}

	@Test
	public void testRemoteFieldReceiver() throws Exception {
		performReceiverTest();
	}

	/* *********************** Import Tests ******************************* */

	private void performImportTest() throws Exception {
		performTestInlineCall(fgTestSetup.getImportPackage(), getName(), COMPARE_WITH_OUTPUT, "import_out");
	}

	@Test
	public void testUseArray() throws Exception {
		performImportTest();
	}

	@Test
	public void testUseInArgument() throws Exception {
		performImportTest();
	}

	@Test
	public void testUseInClassLiteral() throws Exception {
		performImportTest();
	}

	@Test
	public void testUseInDecl() throws Exception {
		performImportTest();
	}

	@Test
	public void testUseInDecl2() throws Exception {
		performImportTest();
	}

	@Test
	public void testUseInDecl3() throws Exception {
		performImportTest();
	}

	@Test
	public void testUseInDeclClash() throws Exception {
		performImportTest();
	}

	@Test
	public void testUseInLocalClass() throws Exception {
		performImportTest();
	}

	@Test
	public void testStaticImport() throws Exception {
		performImportTest();
	}

	@Test
	public void testStaticImport2() throws Exception {
		if (BUG_82166) {
			System.out.println("Disabled static import test 2 due to bug 82166");
			return;
		}
		performImportTest();
	}

	/* *********************** Cast Tests ******************************* */

	private void performCastTest() throws Exception {
		performTestInlineCall(fgTestSetup.getCastPackage(), getName(), COMPARE_WITH_OUTPUT, "cast_out");
	}

	@Test
	public void testNotOverloaded() throws Exception {
		performCastTest();
	}

	@Test
	public void testOverloadedPrimitives() throws Exception {
		performCastTest();
	}

	@Test
	public void testNotCastableOverloaded() throws Exception {
		performCastTest();
	}

	@Test
	public void testOverloaded() throws Exception {
		performCastTest();
	}

	@Test
	public void testHierarchyOverloadedPrimitives() throws Exception {
		performCastTest();
	}

	@Test
	public void testHierarchyOverloaded() throws Exception {
		performCastTest();
	}

	@Test
	public void testHierarchyOverloadedMultiLevel() throws Exception {
		performCastTest();
	}

	@Test
	public void testHierarchyOverloadedPrivate() throws Exception {
		performCastTest();
	}

	@Test
	public void testReceiverCast() throws Exception {
		performCastTest();
	}

	@Test
	public void testNoCast() throws Exception {
		performCastTest();
	}

	@Test
	public void testInfixExpression1() throws Exception {
		performCastTest();
	}

	@Test
	public void testInfixExpression2() throws Exception {
		performCastTest();
	}

	@Test
	public void testReturnValue1() throws Exception {
		performCastTest();
	}

	@Test
	public void testReturnValue2() throws Exception {
		performCastTest();
	}

	@Test
	public void testReturnValue3() throws Exception {
		performCastTest();
	}

	@Test
	public void testReturnValue4() throws Exception {
		performCastTest();
	}

	@Test
	public void testReturnValue5() throws Exception {
		performCastTest();
	}

	/* *********************** Enum Tests ******************************* */

	private void performEnumTest() throws Exception {
		performTestInlineCall(fgTestSetup.getEnumPackage(), getName(), COMPARE_WITH_OUTPUT, "enum_out");
	}

	@Test
	public void testBasic() throws Exception {
		performEnumTest();
	}

	@Test
	public void testAnonymousEnum() throws Exception {
		performEnumTest();
	}

	@Test
	public void test_416198() throws Exception {
		performEnumTest();
	}

	@Test
	public void test_138952() throws Exception {
		performEnumTest();
	}

	/* *********************** Generic Tests ******************************* */

	private void performGenericTest() throws Exception {
		performTestInlineCall(fgTestSetup.getGenericPackage(), getName(), COMPARE_WITH_OUTPUT, "generic_out");
	}

	private void performGenericTestInlineMethod() throws Exception {
		performTestInlineMethod(fgTestSetup.getGenericPackage(), getName(), COMPARE_WITH_OUTPUT, "generic_out");
	}

	@Test
	public void testClassInstance() throws Exception {
		performGenericTest();
	}

	@Test
	public void testClassInstance2() throws Exception {
		performGenericTestInlineMethod();
	}

	@Test
	public void testSubClass1() throws Exception {
		performGenericTest();
	}

	@Test
	public void testSubClass2() throws Exception {
		performGenericTest();
	}

	@Test
	public void testMethodInstance1() throws Exception {
		performGenericTest();
	}

	@Test
	public void testMethodInstance2() throws Exception {
		performGenericTest();
	}

	@Test
	public void testMethodInstance3() throws Exception {
		performGenericTestInlineMethod();
	}

	@Test
	public void testParameterizedType1() throws Exception {
		performGenericTest();
	}

	@Test
	public void testParameterizedType2() throws Exception {
		performGenericTest();
	}

	@Test
	public void testParameterizedType3() throws Exception {
		performGenericTest();
	}

	@Test
	public void testParameterizedType4() throws Exception {
		performGenericTest();
	}

	@Test
	public void testParameterizedType5() throws Exception {
		performGenericTest();
	}

	@Test
	public void testParameterizedType6() throws Exception {
		performGenericTest();
	}

	@Test
	public void testParameterizedMethod() throws Exception {
		performGenericTest();
	}

	/* *********************** Binary Tests ******************************* */

	@Test
	public void testBinaryInlineSingle() throws Exception { // uses classes.Target#
		performTestInlineCall(fgTestSetup.getBinaryPackage(), getName(), COMPARE_WITH_OUTPUT, "binary_out");
	}

	@Test
	public void testBinaryInlineAll() throws Exception { // inlines all classes.Target2#logMessage(..)
		String id= getName();
		ICompilationUnit unit= createCU(fgTestSetup.getBinaryPackage(), id);
		IType target2type= unit.getJavaProject().findType("classes.Target2");
		IClassFile target2ClassFile= target2type.getClassFile();
		IMethod logMessage= target2type.getMethods()[1]; // method 0 is ctor
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(
				target2ClassFile,
				new RefactoringASTParser(AST.getJLSLatest()).parse(target2ClassFile, true),
				logMessage.getNameRange().getOffset(),
				logMessage.getNameRange().getLength());

		assertFalse(refactoring.canEnableDeleteSource());
		refactoring.setCurrentMode(InlineMethodRefactoring.Mode.INLINE_ALL);

		performTest(unit, refactoring, COMPARE_WITH_OUTPUT, getProofedContent("binary_out", id), true);
	}

	@Test
	public void testBinaryNoSource() throws Exception {
		performTestInlineCall(fgTestSetup.getBinaryPackage(), getName(), INVALID_SELECTION, null);
	}

	@Test
	public void test_133575() throws Exception { // uses classes.BinEnum
		performTestInlineCall(fgTestSetup.getBinaryPackage(), getName(), COMPARE_WITH_OUTPUT, "binary_out");
	}

	/* *********************** Operator Tests ******************************* */

	private void performOperatorTest() throws Exception {
		performTestInlineCall(fgTestSetup.getOperatorPackage(), getName(), COMPARE_WITH_OUTPUT, "operator_out");
	}

	@Test
	public void testPlusPlus() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testPlusPlus_1() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testPlusDiff() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testDiffDiff() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testDiffPlus() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testTimesPlus() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testPrefixPlus() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testPostfixPlus() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testPlusTimes() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testPlusPrefix() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testPlusPostfix() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testTimesTimes() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testTimesDivide() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testDivideDivide() throws Exception {
		performOperatorTest();
	}

	@Test
	public void testOperatorPredence() throws Exception {
		AST ast= AST.newAST(AST.getJLSLatest(), false);

		int assignment= OperatorPrecedence.getExpressionPrecedence(ast.newAssignment());
		int conditional= OperatorPrecedence.getExpressionPrecedence(ast.newConditionalExpression());
		assertTrue(assignment < conditional);

		InfixExpression exp= ast.newInfixExpression();
		exp.setOperator(Operator.CONDITIONAL_OR);
		int conditional_or= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(conditional < conditional_or);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.CONDITIONAL_AND);
		int conditional_and= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(conditional_or < conditional_and);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.OR);
		int bitwiseInclusiveOR= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(conditional_and < bitwiseInclusiveOR);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.XOR);
		int bitwiseEnclusiveOR= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(bitwiseInclusiveOR < bitwiseEnclusiveOR);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.AND);
		int bitwiseAnd= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(bitwiseEnclusiveOR < bitwiseAnd);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.EQUALS);
		int equals= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(bitwiseAnd < equals);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.NOT_EQUALS);
		int notEquals= OperatorPrecedence.getExpressionPrecedence(exp);
		assertEquals(equals, notEquals);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.LESS);
		int less= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(notEquals < less);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.LESS_EQUALS);
		int lessEquals= OperatorPrecedence.getExpressionPrecedence(exp);
		assertEquals(less, lessEquals);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.GREATER);
		int greater= OperatorPrecedence.getExpressionPrecedence(exp);
		assertEquals(lessEquals, greater);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.GREATER_EQUALS);
		int greaterEquals= OperatorPrecedence.getExpressionPrecedence(exp);
		assertEquals(greater, greaterEquals);

		int instance= OperatorPrecedence.getExpressionPrecedence(ast.newInstanceofExpression());
		assertEquals(greaterEquals, instance);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.LEFT_SHIFT);
		int leftShift= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(instance < leftShift);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.RIGHT_SHIFT_SIGNED);
		int rightShiftSigned= OperatorPrecedence.getExpressionPrecedence(exp);
		assertEquals(leftShift, rightShiftSigned);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.RIGHT_SHIFT_UNSIGNED);
		int rightShiftUnSigned= OperatorPrecedence.getExpressionPrecedence(exp);
		assertEquals(rightShiftSigned, rightShiftUnSigned);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.PLUS);
		int plus= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(rightShiftUnSigned < plus);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.MINUS);
		int minus= OperatorPrecedence.getExpressionPrecedence(exp);
		assertEquals(plus, minus);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.TIMES);
		int times= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(minus < times);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.DIVIDE);
		int divide= OperatorPrecedence.getExpressionPrecedence(exp);
		assertEquals(times, divide);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.REMAINDER);
		int remainder= OperatorPrecedence.getExpressionPrecedence(exp);
		assertEquals(divide, remainder);

		int cast= OperatorPrecedence.getExpressionPrecedence(ast.newCastExpression());
		assertTrue(times < cast);

		int prefix= OperatorPrecedence.getExpressionPrecedence(ast.newPrefixExpression());
		assertTrue(cast < prefix);

		int postfix= OperatorPrecedence.getExpressionPrecedence(ast.newPostfixExpression());
		assertTrue(prefix < postfix);

		int newClass= OperatorPrecedence.getExpressionPrecedence(ast.newClassInstanceCreation());
		assertEquals(postfix, newClass);
	}
}
