/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.COMPARE_WITH_OUTPUT;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.code.ReplaceInvocationsRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

public class ReplaceInvocationsTests extends AbstractJunit4SelectionTestCase {

	@Rule
	public RewriteMethodInvocationsTestSetup fgTestSetup= new RewriteMethodInvocationsTestSetup();

	public ReplaceInvocationsTests() {
		super(true);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		fIsPreDeltaTest= true;
	}

	@Override
	protected String getResourceLocation() {
		return "ReplaceInvocationsWorkspace/ReplaceInvocations/";
	}

	@Override
	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}

	private void performTestRewriteInvocations(IPackageFragment packageFragment, String id, TestMode mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		int[] selection= getSelection();

		CompilationUnit compilationUnit= new RefactoringASTParser(AST.getJLSLatest()).parse(unit, false);
		Comment comment= (Comment) compilationUnit.getCommentList().get(0);
		String commentString= unit.getBuffer().getText(comment.getStartPosition(), comment.getLength());
		Matcher matcher= Pattern.compile("""
			(?s)/\\*\\s*params:[[^\\r\\n]&&\\s]*\
			([^\\r\\n]*)\
			(\\r\\n?|\\n)\
			(.+)\
			\\*/""").matcher(commentString);
		assertTrue(matcher.find());
		String paramsString= matcher.group(1);
		String[] params= paramsString.length() == 0 ? new String[0] : paramsString.split("[\\s,]+");
		String body= matcher.group(3);

		ReplaceInvocationsRefactoring refactoring= new ReplaceInvocationsRefactoring(unit, selection[0], selection[1]);
		refactoring.setBody(body, params);

		String out= null;
		if (mode == COMPARE_WITH_OUTPUT)
			out= getProofedContent(outputFolder, id);

		performTest(unit, refactoring, mode, out, true);
	}

	/* *********************** Rewrite Invocations Tests ******************************* */

	private void performRewriteTest() throws Exception {
		performTestRewriteInvocations(fgTestSetup.getRewritePackage(), getName(), COMPARE_WITH_OUTPUT, "rewrite_out");
	}

	@Test
	public void testSwitchParameters() throws Exception {
		performRewriteTest();
	}

	@Test
	public void testClassFile() throws Exception {
		performRewriteTest();
	}

	@Test
	public void testMultiple() throws Exception {
		performRewriteTest();
	}

}
