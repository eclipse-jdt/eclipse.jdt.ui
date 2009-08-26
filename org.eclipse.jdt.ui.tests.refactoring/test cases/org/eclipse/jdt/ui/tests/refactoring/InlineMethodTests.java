/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.OperatorPrecedence;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

public class InlineMethodTests extends AbstractSelectionTestCase {
	private static InlineMethodTestSetup fgTestSetup;
	private static final boolean BUG_82166= true;

	public InlineMethodTests(String name) {
		super(name, true);
	}

	public static Test suite() {
		fgTestSetup= new InlineMethodTestSetup(new TestSuite(InlineMethodTests.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test someTest) {
		fgTestSetup= new InlineMethodTestSetup(someTest);
		return fgTestSetup;
	}

	protected void setUp() throws Exception {
		super.setUp();
		fIsPreDeltaTest= true;
	}

	protected String getResourceLocation() {
		return "InlineMethodWorkspace/TestCases/";
	}

	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}

	protected void performTestInlineCall(IPackageFragment packageFragment, String id, int mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		int[] selection= getSelection();
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(unit, new RefactoringASTParser(AST.JLS3).parse(unit, true), selection[0], selection[1]);
		String out= null;
		switch (mode) {
			case COMPARE_WITH_OUTPUT:
				out= getProofedContent(outputFolder, id);
				break;
		}
		performTest(unit, refactoring, mode, out, true);
	}

	private void performTestInlineMethod(IPackageFragment packageFragment, String id, int mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		IType type= unit.getTypes()[0];
		IMethod method= getMethodToInline(type);
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(unit, new RefactoringASTParser(AST.JLS3).parse(unit, true), method.getNameRange().getOffset(), method.getNameRange().getLength());
		String out= null;
		switch (mode) {
			case COMPARE_WITH_OUTPUT:
				out= getProofedContent(outputFolder, id);
				break;
		}
		performTest(unit, refactoring, mode, out, true);
	}

	private IMethod getMethodToInline(IType type) throws CoreException {
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			if ("toInline".equals(methods[i].getElementName()))
				return methods[i];
		}
		return null;
	}

	private void performTestInlineFirstConstructor(IPackageFragment packageFragment, String id, int mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		IType type= unit.getTypes()[0];
		IMethod method= getFirstConstructor(type);
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(unit, new RefactoringASTParser(AST.JLS3).parse(unit, true), method.getNameRange().getOffset(), method.getNameRange().getLength());
		String out= null;
		switch (mode) {
			case COMPARE_WITH_OUTPUT:
				out= getProofedContent(outputFolder, id);
				break;
		}
		performTest(unit, refactoring, mode, out, true);
	}

	private IMethod getFirstConstructor(IType type) throws CoreException {
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			if (methods[i].isConstructor())
				return methods[i];
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

	public void testRecursion() throws Exception {
		performInvalidTest();
	}

	public void testInvalidFieldInitializer1() throws Exception {
		performInvalidTest();
	}

	public void testInvalidFieldInitializer2() throws Exception {
		performInvalidTest();
	}

	public void testInvalidFieldInitializer3() throws Exception {
		performInvalidTest();
	}

	public void testLocalInitializer() throws Exception {
		performInvalidTest();
	}

	public void testInterruptedStatement() throws Exception {
		performInvalidTest();
	}

	public void testInterruptedExecutionFlow() throws Exception {
		performInvalidTest();
	}

	public void testMultiLocal() throws Exception {
		performInvalidTest();
	}

	public void testComplexBody() throws Exception {
		performInvalidTest();
	}

	public void testCompileError1() throws Exception {
		performInvalidTest();
	}

	public void testCompileError2() throws Exception {
		performInvalidTest();
	}

	public void testCompileError3() throws Exception {
		performInvalidTest();
	}

	public void testMultipleMethods() throws Exception {
		performInvalidTestInlineMethod();
	}

	public void testSuperInThis() throws Exception {
		performInvalidTestInlineMethod();
	}

	public void testNotMethodName() throws Exception {
		ICompilationUnit unit= createCU(fgTestSetup.getInvalidPackage(), getName());
		int[] selection= getSelection();
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(unit, new RefactoringASTParser(AST.JLS3).parse(unit, true), selection[0], selection[1]);
		assertNull(refactoring);
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

	public void testBasic1() throws Exception {
		performSimpleTest();
	}

	public void testBasic2() throws Exception {
		performSimpleTest();
	}

	public void testEmptyBody() throws Exception {
		performSimpleTest();
	}

	public void testPrimitiveArray() throws Exception {
		performSimpleTest();
	}

	public void testTypeArray() throws Exception {
		performSimpleTest();
	}

	public void testInitializer() throws Exception {
		performSimpleTest();
	}

	public void testSuper() throws Exception {
		performSimpleTest();
	}

	public void testFieldInitializer1() throws Exception {
		performSimpleTest();
	}

	public void testFieldInitializer2() throws Exception {
		performSimpleTest();
	}

	public void testFieldInitializerAnonymous() throws Exception {
		performSimpleTest();
	}

	public void testLabeledStatement() throws Exception {
		performSimpleTest();
	}

	public void testConstructor1() throws Exception {
		performSimpleTest();
	}

	public void testConstructor2() throws Exception {
		performSimpleTestInlineConstrcutor();
	}

	public void testCatchClause() throws Exception {
		performSimpleTest();
	}

	public void testTwoCalls() throws Exception {
		performSimpleTestInlineMethod();
	}

	public void testNestedCalls() throws Exception {
		performSimpleTestInlineMethod();
	}

	public void testSurroundingCallers() throws Exception {
		performSimpleTestInlineMethod();
	}

	public void testComment1() throws Exception {
		performSimpleTestInlineMethod();
	}

	/* *********************** Bug Tests ******************************* */

	private void performBugTest() throws Exception {
		performTestInlineCall(fgTestSetup.getBugsPackage(), getName(), COMPARE_WITH_OUTPUT, "bugs_out");
	}

	private void performBugTestInlineMethod() throws Exception {
		performTestInlineMethod(fgTestSetup.getBugsPackage(), getName(), COMPARE_WITH_OUTPUT, "bugs_out");
	}

	public void test_72836() throws Exception {
		performBugTest();
	}

	public void test_76241() throws Exception {
		performBugTestInlineMethod();
	}

	public void test_94426() throws Exception {
		performBugTestInlineMethod();
	}

	public void test_95128() throws Exception {
		performBugTestInlineMethod();
	}

	public void test_117053() throws Exception {
		performBugTest();
	}

	public void test_123356() throws Exception {
		performBugTest();
	}

	public void test_44419() throws Exception {
		performBugTest();
	}

	public void test_44419_2() throws Exception {
		performBugTest();
	}

	public void test_98856() throws Exception {
		performBugTest();
	}

	public void test_50139() throws Exception {
		performBugTest();
	}

	public void test_287378() throws Exception {
		performBugTest();
	}
	
	/* *********************** Argument Tests ******************************* */

	private void performArgumentTest() throws Exception {
		performTestInlineCall(fgTestSetup.getArgumentPackage(), getName(), COMPARE_WITH_OUTPUT, "argument_out");
	}

	public void testFieldReference() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceUnused() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceRead() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceRead2() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceWrite() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceLoop() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceLoop1() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceLoop2() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceLoop3() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceLoop4() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferenceLoop5() throws Exception {
		performArgumentTest();
	}

	public void testLocalReferencePrefix() throws Exception {
		performArgumentTest();
	}

	public void testLiteralReferenceRead() throws Exception {
		performArgumentTest();
	}

	public void testLiteralReferenceWrite() throws Exception {
		performArgumentTest();
	}

	public void testParameterNameUsed1() throws Exception {
		performArgumentTest();
	}

	public void testParameterNameUsed2() throws Exception {
		performArgumentTest();
	}

	public void testParameterNameUsed3() throws Exception {
		performArgumentTest();
	}

	public void testParameterNameUsed4() throws Exception {
		performArgumentTest();
	}

	public void testParameterNameUnused1() throws Exception {
		performArgumentTest();
	}

	public void testParameterNameUnused2() throws Exception {
		performArgumentTest();
	}

	public void testParameterNameUnused3() throws Exception {
		performArgumentTest();
	}

	public void testOneRead() throws Exception {
		performArgumentTest();
	}

	public void testTwoReads() throws Exception {
		performArgumentTest();
	}

	public void testWrite() throws Exception {
		performArgumentTest();
	}

	public void testArray() throws Exception {
		performArgumentTest();
	}

	public void testVarargs() throws Exception {
		performArgumentTest();
	}

	public void testVarargs2() throws Exception {
		performArgumentTest();
	}

	public void testVarargs3() throws Exception {
		performArgumentTest();
	}

	public void testVarargs4() throws Exception {
		performArgumentTest();
	}

	public void testVarargs5() throws Exception {
		performArgumentTest();
	}

	public void testVarargs6() throws Exception {
		performArgumentTest();
	}

	public void test91470() throws Exception {
		performArgumentTest();
	}

	/* *********************** Name Conflict Tests ******************************* */

	private void performNameConflictTest() throws Exception {
		performTestInlineCall(fgTestSetup.getNameConflictPackage(), getName(), COMPARE_WITH_OUTPUT, "nameconflict_out");
	}

	public void testSameLocal() throws Exception {
		performNameConflictTest();
	}

	public void testSameType() throws Exception {
		performNameConflictTest();
	}

	public void testSameTypeAfter() throws Exception {
		performNameConflictTest();
	}

	public void testSameTypeInSibling() throws Exception {
		performNameConflictTest();
	}

	public void testLocalInType() throws Exception {
		performNameConflictTest();
	}

	public void testFieldInType() throws Exception {
		performNameConflictTest();
	}

	public void testSwitchStatement() throws Exception {
		performNameConflictTest();
	}

	public void testBlocks() throws Exception {
		performNameConflictTest();
	}

	/* *********************** Call Tests ******************************* */

	private void performCallTest() throws Exception {
		performTestInlineCall(fgTestSetup.getCallPackage(), getName(), COMPARE_WITH_OUTPUT, "call_out");
	}

	public void testExpressionStatement() throws Exception {
		performCallTest();
	}

	public void testExpressionStatementWithReturn() throws Exception {
		performCallTest();
	}

	public void testStatementWithFunction1() throws Exception {
		performCallTest();
	}

	public void testStatementWithFunction2() throws Exception {
		performCallTest();
	}

	public void testParenthesis() throws Exception {
		performCallTest();
	}

	/* *********************** Expression Tests ******************************* */

	private void performExpressionTest() throws Exception {
		performTestInlineCall(fgTestSetup.getExpressionPackage(), getName(), COMPARE_WITH_OUTPUT, "expression_out");
	}

	public void testSimpleExpression() throws Exception {
		performExpressionTest();
	}

	public void testSimpleExpressionWithStatements() throws Exception {
		performExpressionTest();
	}

	public void testSimpleBody() throws Exception {
		performExpressionTest();
	}

	public void testAssignment() throws Exception {
		performExpressionTest();
	}

	public void testReturnStatement() throws Exception {
		performExpressionTest();
	}

	public void testConditionalExpression() throws Exception {
		performExpressionTest();
	}

	/* *********************** Control Statements Tests ******************************* */

	private void performControlStatementTest() throws Exception {
		performTestInlineCall(fgTestSetup.getControlStatementPackage(), getName(), COMPARE_WITH_OUTPUT, "controlStatement_out");
	}

	public void testForEmpty() throws Exception {
		performControlStatementTest();
	}

	public void testForOne() throws Exception {
		performControlStatementTest();
	}

	public void testForTwo() throws Exception {
		performControlStatementTest();
	}

	public void testEnhancedForOne() throws Exception {
		performControlStatementTest();
	}

	public void testEnhancedForTwo() throws Exception {
		performControlStatementTest();
	}

	public void testIfThenTwo() throws Exception {
		performControlStatementTest();
	}

	public void testIfElseTwo() throws Exception {
		performControlStatementTest();
	}

	public void testForAssignmentOne() throws Exception {
		performControlStatementTest();
	}

	public void testForAssignmentTwo() throws Exception {
		performControlStatementTest();
	}

	public void testLabelOne() throws Exception {
		performControlStatementTest();
	}

	public void testLabelTwo() throws Exception {
		performControlStatementTest();
	}

	public void testDanglingIf() throws Exception {
		performControlStatementTest();
	}

	public void testIfWithVariable() throws Exception {
		performControlStatementTest();
	}

	public void testDanglingIfBug229734() throws Exception {
		performControlStatementTest();
	}

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

	public void testNoImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	public void testNameThisReceiver() throws Exception {
		performReceiverTest();
	}

	public void testNameImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	public void testExpressionZeroImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	public void testExpressionOneImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	public void testExpressionTwoImplicitReceiver() throws Exception {
		performReceiverTest();
	}

	public void testStaticReceiver() throws Exception {
		performReceiverTest();
	}

	public void testReceiverWithStatic() throws Exception {
		performReceiverTest();
	}

	public void testThisExpression() throws Exception {
		performReceiverTest();
	}

	public void testFieldReceiver() throws Exception {
		performReceiverTest();
	}

	public void testExplicitStaticThisFieldReceiver() throws Exception {
		performReceiverTest();
	}

	public void testExplicitThisFieldReceiver() throws Exception {
		performReceiverTest();
	}

	public void testExplicitStaticThisMethodReceiver() throws Exception {
		performReceiverTest();
	}

	public void testExplicitThisMethodReceiver() throws Exception {
		performReceiverTest();
	}

	public void testThisReceiver() throws Exception {
		performReceiverTestInlineMethod();
	}

	public void testImplicitReceiverMethod() throws Exception {
		performReceiverTest();
	}

	public void testImplicitReceiverField() throws Exception {
		performReceiverTest();
	}

	public void testRemoteFieldReceiver() throws Exception {
		performReceiverTest();
	}

	/* *********************** Import Tests ******************************* */

	private void performImportTest() throws Exception {
		performTestInlineCall(fgTestSetup.getImportPackage(), getName(), COMPARE_WITH_OUTPUT, "import_out");
	}

	public void testUseArray() throws Exception {
		performImportTest();
	}

	public void testUseInArgument() throws Exception {
		performImportTest();
	}

	public void testUseInClassLiteral() throws Exception {
		performImportTest();
	}

	public void testUseInDecl() throws Exception {
		performImportTest();
	}

	public void testUseInDecl2() throws Exception {
		performImportTest();
	}

	public void testUseInDecl3() throws Exception {
		performImportTest();
	}

	public void testUseInDeclClash() throws Exception {
		performImportTest();
	}

	public void testUseInLocalClass() throws Exception {
		performImportTest();
	}

	public void testStaticImport() throws Exception {
		performImportTest();
	}

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

	public void testNotOverloaded() throws Exception {
		performCastTest();
	}

	public void testOverloadedPrimitives() throws Exception {
		performCastTest();
	}

	public void testNotCastableOverloaded() throws Exception {
		performCastTest();
	}

	public void testOverloaded() throws Exception {
		performCastTest();
	}

	public void testHierarchyOverloadedPrimitives() throws Exception {
		performCastTest();
	}

	public void testHierarchyOverloaded() throws Exception {
		performCastTest();
	}

	public void testHierarchyOverloadedPrivate() throws Exception {
		performCastTest();
	}

	public void testReceiverCast() throws Exception {
		performCastTest();
	}

	public void testNoCast() throws Exception {
		performCastTest();
	}

	/* *********************** Enum Tests ******************************* */

	private void performEnumTest() throws Exception {
		performTestInlineCall(fgTestSetup.getEnumPackage(), getName(), COMPARE_WITH_OUTPUT, "enum_out");
	}

	public void testBasic() throws Exception {
		performEnumTest();
	}

	public void testAnonymousEnum() throws Exception {
		performEnumTest();
	}

	/* *********************** Generic Tests ******************************* */

	private void performGenericTest() throws Exception {
		performTestInlineCall(fgTestSetup.getGenericPackage(), getName(), COMPARE_WITH_OUTPUT, "generic_out");
	}

	private void performGenericTestInlineMethod() throws Exception {
		performTestInlineMethod(fgTestSetup.getGenericPackage(), getName(), COMPARE_WITH_OUTPUT, "generic_out");
	}

	public void testClassInstance() throws Exception {
		performGenericTest();
	}

	public void testClassInstance2() throws Exception {
		performGenericTestInlineMethod();
	}

	public void testSubClass1() throws Exception {
		performGenericTest();
	}

	public void testSubClass2() throws Exception {
		performGenericTest();
	}

	public void testMethodInstance1() throws Exception {
		performGenericTest();
	}

	public void testMethodInstance2() throws Exception {
		performGenericTest();
	}

	public void testMethodInstance3() throws Exception {
		performGenericTestInlineMethod();
	}

	public void testParameterizedType1() throws Exception {
		performGenericTest();
	}

	public void testParameterizedType2() throws Exception {
		performGenericTest();
	}

	public void testParameterizedType3() throws Exception {
		performGenericTest();
	}

	/* *********************** Binary Tests ******************************* */

	public void testBinaryInlineSingle() throws Exception { // uses classes.Target#
		performTestInlineCall(fgTestSetup.getBinaryPackage(), getName(), COMPARE_WITH_OUTPUT, "binary_out");
	}

	public void testBinaryInlineAll() throws Exception { // inlines all classes.Target2#logMessage(..)
		String id= getName();
		ICompilationUnit unit= createCU(fgTestSetup.getBinaryPackage(), id);
		IType target2type= unit.getJavaProject().findType("classes.Target2");
		IClassFile target2ClassFile= target2type.getClassFile();
		IMethod logMessage= target2type.getMethods()[1]; // method 0 is ctor
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(
				target2ClassFile,
				new RefactoringASTParser(AST.JLS3).parse(target2ClassFile, true),
				logMessage.getNameRange().getOffset(),
				logMessage.getNameRange().getLength());

		assertFalse(refactoring.canEnableDeleteSource());
		refactoring.setCurrentMode(InlineMethodRefactoring.Mode.INLINE_ALL);

		String out= null;
		switch (COMPARE_WITH_OUTPUT) {
			case COMPARE_WITH_OUTPUT:
				out= getProofedContent("binary_out", id);
				break;
		}
		performTest(unit, refactoring, COMPARE_WITH_OUTPUT, out, true);
	}

	public void testBinaryNoSource() throws Exception {
		performTestInlineCall(fgTestSetup.getBinaryPackage(), getName(), INVALID_SELECTION, null);
	}

	public void test_133575() throws Exception { // uses classes.BinEnum
		performTestInlineCall(fgTestSetup.getBinaryPackage(), getName(), COMPARE_WITH_OUTPUT, "binary_out");
	}

	/* *********************** Operator Tests ******************************* */

	private void performOperatorTest() throws Exception {
		performTestInlineCall(fgTestSetup.getOperatorPackage(), getName(), COMPARE_WITH_OUTPUT, "operator_out");
	}

	public void testPlusPlus() throws Exception {
		performOperatorTest();
	}

	public void testTimesPlus() throws Exception {
		performOperatorTest();
	}

	public void testPrefixPlus() throws Exception {
		performOperatorTest();
	}

	public void testPostfixPlus() throws Exception {
		performOperatorTest();
	}

	public void testPlusTimes() throws Exception {
		performOperatorTest();
	}

	public void testPlusPrefix() throws Exception {
		performOperatorTest();
	}

	public void testPlusPostfix() throws Exception {
		performOperatorTest();
	}

	public void testOperatorPredence() throws Exception {
		AST ast= AST.newAST(AST.JLS3);

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
		assertTrue(equals == notEquals);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.LESS);
		int less= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(notEquals < less);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.LESS_EQUALS);
		int lessEquals= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(less == lessEquals);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.GREATER);
		int greater= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(lessEquals == greater);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.GREATER_EQUALS);
		int greaterEquals= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(greater == greaterEquals);

		int instance= OperatorPrecedence.getExpressionPrecedence(ast.newInstanceofExpression());
		assertTrue(greaterEquals == instance);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.LEFT_SHIFT);
		int leftShift= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(instance < leftShift);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.RIGHT_SHIFT_SIGNED);
		int rightShiftSigned= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(leftShift == rightShiftSigned);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.RIGHT_SHIFT_UNSIGNED);
		int rightShiftUnSigned= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(rightShiftSigned == rightShiftUnSigned);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.PLUS);
		int plus= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(rightShiftUnSigned < plus);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.MINUS);
		int minus= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(plus == minus);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.TIMES);
		int times= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(minus < times);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.DIVIDE);
		int divide= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(times == divide);

		exp= ast.newInfixExpression();
		exp.setOperator(Operator.REMAINDER);
		int remainder= OperatorPrecedence.getExpressionPrecedence(exp);
		assertTrue(divide == remainder);

		int cast= OperatorPrecedence.getExpressionPrecedence(ast.newCastExpression());
		assertTrue(times < cast);

		int prefix= OperatorPrecedence.getExpressionPrecedence(ast.newPrefixExpression());
		assertTrue(cast < prefix);

		int postfix= OperatorPrecedence.getExpressionPrecedence(ast.newPostfixExpression());
		assertTrue(prefix < postfix);

		int newClass= OperatorPrecedence.getExpressionPrecedence(ast.newClassInstanceCreation());
		assertTrue(postfix == newClass);
	}
}
