/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [extract local] Extract to local variable not replacing multiple occurrences in same statement - https://bugs.eclipse.org/406347
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractConstantRefactoring;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ExtractConstantTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH = "ExtractConstant/";

	private String fCompactPref;
	private boolean fAddComments;

	public ExtractConstantTests() {
		this.rts= new RefactoringTestSetup();
	}

	protected ExtractConstantTests(RefactoringTestSetup rts) {
		super(rts);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	protected String getSimpleTestFileName(boolean canInline, boolean input) {
		StringBuilder fileName = new StringBuilder("A_").append(getName());
		if (canInline)
			fileName.append(input ? "_in": "_out");
		return fileName.append(".java").toString();
	}

	protected String getTestFileName(boolean canExtract, boolean input) {
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canExtract ? "canExtract/": "cannotExtract/");
		return fileName.append(getSimpleTestFileName(canExtract, input)).toString();
	}

	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canExtract, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canExtract, input), getFileContents(getTestFileName(canExtract, input)));
	}

	@Override
	public void genericbefore() throws Exception {
		super.genericbefore();
		Hashtable<String, String> options= JavaCore.getOptions();

		String setting= DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR;
		fCompactPref= options.get(setting);
		options.put(setting, DefaultCodeFormatterConstants.TRUE);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		fAddComments= store.getBoolean(PreferenceConstants.CODEGEN_ADD_COMMENTS);
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
	}

	@Override
	public void genericafter() throws Exception {
		super.genericafter();
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, fCompactPref);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, fAddComments);
	}

	private void guessHelper(int startLine, int startColumn, int endLine, int endColumn, String expectedGuessedName) throws Exception {
		ICompilationUnit cu= createCU(getPackageP(), getName()+".java", getFileContents(TEST_PATH_PREFIX + getRefactoringPath() + "nameGuessing/" + getName()+".java"));
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ExtractConstantRefactoring ref= new ExtractConstantRefactoring(cu, selection.getOffset(), selection.getLength());
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("activation was supposed to be successful" + preconditionResult.toString(), preconditionResult.isOK());
		assertEquals("constant name not guessed", expectedGuessedName, ref.guessConstantName());
	}

	private void helper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean allowLoadtime, boolean qualifyReferencesWithConstantName, String constantName, String guessedConstantName) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ExtractConstantRefactoring ref= new ExtractConstantRefactoring(cu, selection.getOffset(), selection.getLength());
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("activation was supposed to be successful" + preconditionResult.toString(), preconditionResult.isOK());

		if(!allowLoadtime)
			assertTrue("The selected expression has been erroneously reported to contain references to non-static or non-final fields.", ref.selectionAllStaticFinal());

		ref.setReplaceAllOccurrences(replaceAll);
		ref.setQualifyReferencesWithDeclaringClassName(qualifyReferencesWithConstantName);
		ref.setConstantName(constantName);

		assertEquals("constant name incorrectly guessed", guessedConstantName, ref.guessConstantName());

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
		assertTrue("precondition was supposed to pass", checkInputResult.isOK());

		performChange(ref, false);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEqualLines(getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	protected void helper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean allowLoadtime, String constantName, String guessedConstantName) throws Exception {
		helper1(startLine, startColumn, endLine, endColumn, replaceAll, allowLoadtime, false, constantName, guessedConstantName);
	}

	protected void failHelper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean allowLoadtime, String constantName) throws Exception {
		failHelper1(startLine, startColumn, endLine, endColumn, replaceAll, allowLoadtime, constantName, 0, false);
	}

	private void failHelper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean allowLoadtime, String constantName, int errorCode, boolean checkCode) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ExtractConstantRefactoring ref= new ExtractConstantRefactoring(cu, selection.getOffset(), selection.getLength());
		ref.setReplaceAllOccurrences(replaceAll);
		ref.setConstantName(constantName);
		RefactoringStatus result= performRefactoring(ref);

		if(!allowLoadtime && !ref.selectionAllStaticFinal())
			return;

		assertNotNull("precondition was supposed to fail", result);
		if(checkCode)
			assertEquals(errorCode, result.getEntryMatchingSeverity(RefactoringStatus.ERROR).getCode());
	}

	//--- TESTS

	@Test
	public void test0() throws Exception {
		helper1(5, 16, 5, 17, true, false, "CONSTANT", "_0");
	}

	@Test
	public void test1() throws Exception {
		helper1(5, 16, 5, 21, false, false, "CONSTANT", "INT");
	}

	@Test
	public void test2() throws Exception {
		helper1(8, 16, 8, 27, false, false, "CONSTANT", "INT");
	}

	@Test
	public void test3() throws Exception {
		helper1(8, 16, 8, 27, true, false, "CONSTANT", "INT");
	}

	@Test
	public void test4() throws Exception {
		helper1(5, 23, 5, 34, true, false, "CONSTANT", "INT");
	}

	@Test
	public void test5() throws Exception {
		helper1(11, 20, 11, 26, true, true, "CONSTANT", "R_G");
	}

	@Test
	public void test6() throws Exception {
		helper1(13, 20, 13, 35, true, true, "CONSTANT", "R_F");
	}

	@Test
	public void test7() throws Exception {
		helper1(12, 20, 12, 28, true, true, "CONSTANT", "R_G");
	}

	@Test
	public void test8() throws Exception {
		helper1(8, 16, 8, 22, true, true, "CONSTANT", "INT");
	}

	@Test
	public void test9() throws Exception {
		helper1(6, 24, 6, 29, true, true, "CONSTANT", "INT");
	}

	@Test
	public void test10() throws Exception {
		helper1(8, 17, 8, 22, true, true, "CONSTANT", "INT");
	}

	@Test
	public void test11() throws Exception {
		helper1(10, 37, 10, 43, true, true, "CONSTANT", "INT");
	}

	@Test
	public void test12() throws Exception {
		helper1(9, 19, 9, 24, true, true, "CONSTANT", "INT");
	}

	@Test
	public void test13() throws Exception{
		helper1(9, 16, 9, 28, true, true, "CONSTANT", "F");
	}

	@Test
	public void test14() throws Exception{
		helper1(10, 28, 10, 44, true, true, "CONSTANT", "INT");
	}

	@Test
	public void test15() throws Exception{
		helper1(5, 16, 5, 25, true, false, "CONSTANT", "FRED");
	}

	@Test
	public void test16() throws Exception{
		helper1(5, 20, 5, 27, true, false, "CONSTANT", "RED");
	}

	@Test
	public void test17() throws Exception{
		helper1(5, 16, 5, 35, true, false, "CONSTANT", "YET_ANOTHER_FRED");
	}

	@Test
	public void test18() throws Exception {
		helper1(5, 16, 5, 17, true, false, true, "CONSTANT", "_0");
	}

	@Test
	public void test19() throws Exception {
		helper1(5, 23, 5, 38, false, false, "CONSTANT", "STRING");
	}

	@Test
	public void test20() throws Exception {
		helper1(7, 19, 7, 28, false, false, "CONSTANT", "STRING");
	}

	@Test
	public void test21() throws Exception {
		helper1(4, 28, 4, 37, false, false, "CONSTANT", "STRING");
	}

	@Test
	public void test22() throws Exception {
		helper1(9, 35, 9, 59, false, false, "ITEMS", "ARRAY_LIST");
	}

	@Test
	public void test23() throws Exception {
		helper1(14, 12, 14, 15, true, false, "COLOR", "RED2");
	}

	@Ignore("BUG_86113_ImportRewrite")
	@Test
	public void test24() throws Exception {
		helper1(9, 28, 9, 36, true, false, "NUM", "ENUM");
	}

	@Test
	public void test25() throws Exception {
		helper1(5, 27, 5, 40, false, false, "DEFAULT_NAME", "JEAN_PIERRE");
	}

	@Test
	public void test26() throws Exception {
		helper1(6, 16, 6, 32, true, false, true, "INT", "A");
	}

	@Test
	public void test27() throws Exception {
		helper1(13, 14, 13, 19, true, false, false, "FOO", "FOO");
	}

	@Test
	public void test28() throws Exception {
		helper1(13, 14, 13, 19, true, false, false, "FOO", "FOO");
	}

	@Test
	public void test29() throws Exception {
		helper1(12, 19, 12, 28, false, true, "NUMBER", "NUMBER");
	}

	@Test
	public void test30() throws Exception {
		helper1(12, 19, 12, 28, false, true, "INTEGER", "INTEGER");
	}

	@Test
	public void test31() throws Exception { //bug 104293
		helper1(9, 32, 9, 44, true, false, "AS_LIST", "AS_LIST");
	}

	@Test
	public void test32() throws Exception { //bug 104293
		helper1(7, 20, 7, 35, true, false, "STRING", "STRING");
	}

	@Test
	public void test33() throws Exception { //bug 108354
		helper1(7, 20, 7, 35, true, false, "STRING", "STRING");
	}

	@Ignore("BUG 405780 [1.8][compiler] Bad syntax error 'insert \":: IdentifierOrNew\"' for missing semicolon")
	@Test
	public void test34() throws Exception { // syntax error
		helper1(7, 20, 7, 35, true, false, "STRING", "STRING");
	}

	@Test
	public void test35() throws Exception { // bug 218108
		helper1(7, 20, 7, 25, true, false, "BUG", "BUG");
	}

	@Test
	public void test36() throws Exception { // bug 218108
		helper1(7, 20, 7, 25, true, false, "BUG", "BUG");
	}

	@Test
	public void test37() throws Exception { // bug 307758
		helper1(6, 17, 6, 24, true, false, "INT", "INT");
	}

	@Test
	public void test38() throws Exception { // bug 317224
		helper1(3, 19, 3, 24, true, false, "S_ALL", "ALL");
	}

	@Test
	public void test39() throws Exception { // bug 335173
		helper1(5, 21, 5, 26, false, false, "CONSTANT", "INT");
	}

	@Test
	public void test40() throws Exception { // bug 335173
		helper1(5, 20, 5, 27, false, false, "CONSTANT", "INT");
	}

	@Test
	public void test41() throws Exception { // bug 335173
		helper1(5, 22, 5, 27, false, false, "CONSTANT", "INT");
	}

	@Test
	public void test42() throws Exception { // bug 335173
		helper1(5, 21, 5, 28, false, false, "CONSTANT", "INT");
	}

	@Test
	public void test43() throws Exception { // bug 335173
		helper1(5, 20, 5, 29, false, false, "CONSTANT", "INT");
	}

	@Test
	public void test44() throws Exception { // bug 211529
		helper1(7, 18, 7, 19, false, false, "CONSTANT", "_1");
	}

	@Test
	public void test45() throws Exception { // bug 406347
		helper1(7, 17, 7, 22, false, false, "CONSTANT", "INT");
	}

	@Test
	public void test46() throws Exception { // bug 406347
		helper1(8, 17, 8, 22, true, false, "CONSTANT", "INT");
	}

	@Test
	public void testZeroLengthSelection0() throws Exception {
		helper1(5, 18, 5, 18, false, false, "CONSTANT", "_100");
	}

	// -- testing failing preconditions
	@Test
	public void testFail0() throws Exception{
		failHelper1(8, 16, 8, 21, true, true, "CONSTANT");
	}

	@Test
	public void testFail1() throws Exception{
		failHelper1(8, 16, 8, 26 , true, true, "CONSTANT");
	}

	@Test
	public void testFail2() throws Exception{
		failHelper1(9, 20, 9, 21 , true, true, "CONSTANT");
	}

	@Test
	public void testFail3() throws Exception{
		failHelper1(9, 18, 9, 25, true, true, "CONSTANT");
	}

	@Test
	public void testFail4() throws Exception{
		failHelper1(6, 16, 6, 20, true, true, "CONSTANT");
	}

	@Test
	public void testFail5() throws Exception{
		failHelper1(9, 16, 9, 25, true, true, "CONSTANT");
	}

	@Test
	public void testFail6() throws Exception{
		failHelper1(11, 20, 11, 24, true, true, "CONSTANT");
	}

	@Test
	public void testFail7() throws Exception{
		failHelper1(11, 20, 11, 34, true, true, "CONSTANT");
	}

	@Test
	public void testFail10() throws Exception{
		failHelper1(15, 20, 15, 37, true, false, "CONSTANT");
	}

	@Test
	public void testFail11() throws Exception{
		failHelper1(8, 16, 8, 22, true, false, "CONSTANT");
	}

	@Test
	public void testFail12() throws Exception{
		failHelper1(4, 7, 4, 8, true, true, "CONSTANT", RefactoringStatusCodes.EXPRESSION_NOT_RVALUE, true);
	}

	@Test
	public void testFail13() throws Exception {
		failHelper1(2, 9, 2, 10, true, true, "CONSTANT", RefactoringStatusCodes.EXPRESSION_NOT_RVALUE, true);
	}

	@Test
	public void testFail14() throws Exception {
		failHelper1(5, 9, 5, 11, true, true, "CONSTANT");
	}

	@Test
	public void testFail15() throws Exception {
		failHelper1(5, 10, 5, 13, true, true, "CONSTANT");
	}

	@Test
	public void testFail16() throws Exception {
		failHelper1(9, 20, 9, 38, true, false, "CONSTANT");
	}

	@Test
	public void testFail17() throws Exception {
		failHelper1(16, 18, 16, 21, true, true, "COLOR");
	}

	@Test
	public void testGuessStringLiteral0() throws Exception {
		//test for bug 37377
		guessHelper(4, 19, 4, 32, "FOO_HASH_MAP");
	}

	@Test
	public void testGuessStringLiteral1() throws Exception {
		//test for bug 37377
		guessHelper(4, 19, 4, 33, "FOO_HASH_MAP");
	}

	@Test
	public void testGuessStringLiteral2() throws Exception {
		//test for bug 37377
		guessHelper(4, 19, 4, 56, "HANS_IM_GLUECK123_34_BLA_BLA");
	}

	@Test
	public void testGuessStringLiteral3() throws Exception {
		guessHelper(5, 16, 5, 16, "ASSUME_CAMEL_CASE");
	}

	@Test
	public void testGuessFromGetterName0() throws Exception {
		guessHelper(4, 19, 4, 30, "FOO_BAR");
	}

	@Test
	public void testGuessFromGetterName1() throws Exception {
		guessHelper(4, 23, 4, 33, "FOO_BAR");
	}
}
