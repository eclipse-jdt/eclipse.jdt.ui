/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;

public class InlineMethodTests extends AbstractSelectionTestCase {
	private static final boolean BUG_79516= true;
	
	private static InlineMethodTestSetup fgTestSetup;
	
	public InlineMethodTests(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new InlineMethodTestSetup(new TestSuite(InlineMethodTests.class));
		return fgTestSetup;
	}
	
	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}	
	
	protected String getResourceLocation() {
		return "InlineMethodWorkspace/TestCases/";
	}
	
	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}
	
	protected void performTest(IPackageFragment packageFragment, String id, int mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		String source= unit.getSource();
		int[] selection= getSelection(source);
		InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(
			unit, selection[0], selection[1],
			JavaPreferencesSettings.getCodeGenerationSettings());
		String out= null;
		switch (mode) {
			case COMPARE_WITH_OUTPUT:
				out= getProofedContent(outputFolder, id);
				break;		
		}
		performTest(unit, refactoring, mode, out, true);
	}

	/* *********************** Invalid Tests ******************************* */
		
	protected void performInvalidTest() throws Exception {
		performTest(fgTestSetup.getInvalidPackage(), getName(), INVALID_SELECTION, null);
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
	
	/* *********************** Simple Tests ******************************* */
		
	private void performSimpleTest() throws Exception {
		performTest(fgTestSetup.getSimplePackage(), getName(), COMPARE_WITH_OUTPUT, "simple_out");
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

	public void testLabeledStatement() throws Exception {
		performSimpleTest();
	}	

	public void testConstructor1() throws Exception {
		performSimpleTest();
	}	

	/* *********************** Argument Tests ******************************* */
		
	private void performArgumentTest() throws Exception {
		performTest(fgTestSetup.getArgumentPackage(), getName(), COMPARE_WITH_OUTPUT, "argument_out");
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
	
	/* *********************** Name Conflict Tests ******************************* */
		
	private void performNameConflictTest() throws Exception {
		performTest(fgTestSetup.getNameConflictPackage(), getName(), COMPARE_WITH_OUTPUT, "nameconflict_out");
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
		performTest(fgTestSetup.getCallPackage(), getName(), COMPARE_WITH_OUTPUT, "call_out");
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
		performTest(fgTestSetup.getExpressionPackage(), getName(), COMPARE_WITH_OUTPUT, "expression_out");
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
		performTest(fgTestSetup.getControlStatementPackage(), getName(), COMPARE_WITH_OUTPUT, "controlStatement_out");
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

	/* *********************** Receiver Tests ******************************* */
		
	private void performReceiverTest() throws Exception {
		performTest(fgTestSetup.getReceiverPackage(), getName(), COMPARE_WITH_OUTPUT, "receiver_out");
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
	
	/* *********************** Import Tests ******************************* */
		
	private void performImportTest() throws Exception {
		performTest(fgTestSetup.getImportPackage(), getName(), COMPARE_WITH_OUTPUT, "import_out");
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
		if (BUG_79516) {
			System.out.println("testUseInLocalClass disabled (bug 79516)");
			return;
		}
		performImportTest();
	}	

	/* *********************** Cast Tests ******************************* */

	private void performCastTest() throws Exception {
		performTest(fgTestSetup.getCastPackage(), getName(), COMPARE_WITH_OUTPUT, "cast_out");
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
}
