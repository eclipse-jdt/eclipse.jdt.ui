/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.rules.FastPartitioner;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.java.SmartSemicolonAutoEditStrategy;

/**
 * SmartSemicolonAutoEditStrategyTest
 * @since 3.0
 */
public class SmartSemicolonAutoEditStrategyTest {

	/**
	 * Testclass exposing the method to be tested
	 */
	private static class SmartSemicolon extends SmartSemicolonAutoEditStrategy {

		public SmartSemicolon() {
			super("");
		}

		public static int computeCharacterPosition(IDocument document, ITextSelection line, int offset, char character, String partitioning) {
			return SmartSemicolonAutoEditStrategy.computeCharacterPosition(document, line, offset, character, partitioning);
		}
	}

	private FastPartitioner fPartitioner;

	private static final char SEMI= ';';
	private static final char BRACE= '{';

	private Document fDocument;

	public SmartSemicolonAutoEditStrategyTest() {
		fDocument= new Document();
		String[] types= new String[] {
			IJavaPartitions.JAVA_DOC,
			IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
			IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
			IJavaPartitions.JAVA_STRING,
			IJavaPartitions.JAVA_CHARACTER,
			IDocument.DEFAULT_CONTENT_TYPE
		};
		fPartitioner= new FastPartitioner(new FastJavaPartitionScanner(), types);
		fPartitioner.connect(fDocument);
		fDocument.setDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING, fPartitioner);
	}

	private void verifySemicolonPosition(int caret, int expected) throws BadLocationException {
		IRegion region= fDocument.getLineInformation(0);
		ITextSelection selection= new TextSelection(fDocument, region.getOffset(), region.getLength());
		int pos= SmartSemicolon.computeCharacterPosition(fDocument, selection, caret, SEMI, IJavaPartitions.JAVA_PARTITIONING);
		assertEquals(expected, pos);
	}

	private void verifyBracePosition(int caret, int expected) throws BadLocationException {
		IRegion region= fDocument.getLineInformation(0);
		ITextSelection selection= new TextSelection(fDocument, region.getOffset(), region.getLength());
		int pos= SmartSemicolon.computeCharacterPosition(fDocument, selection, caret, BRACE, IJavaPartitions.JAVA_PARTITIONING);
		if (pos == -1) pos= caret;
		assertEquals(expected, pos);
	}

	/* semicolon tests */

	@Test
	public void testGoToEOL() throws BadLocationException {
		fDocument.set("public void foobar()");
		verifySemicolonPosition(5, 20);
	}

	@Ignore("testGoToExisting disabled - unwanted functionality")
	@Test
	public void testGoToExisting() throws BadLocationException {
		fDocument.set("public void; foobar()");
		verifySemicolonPosition(5, 11);
	}

	@Test
	public void testFor() throws BadLocationException {
		fDocument.set("for (int i= 0)");
		verifySemicolonPosition(13, -1);
	}

	@Test
	public void testWithExistingBefore() throws BadLocationException {
		fDocument.set("public void; foobar()");
		verifySemicolonPosition(12, 21);
	}

	@Test
	public void testWithExistingBeforeWithComment() throws BadLocationException {
		fDocument.set("public void; foobar() // comment\r\n ");
		verifySemicolonPosition(12, 21);
	}

	@Ignore("testWithExistingAtInsertPosition disabled - existing characters handled by framework")
	@Test
	public void testWithExistingAtInsertPosition() throws BadLocationException {
		fDocument.set("public void foobar(); // comment\r\n");
		verifySemicolonPosition(12, 12);
	}

	@Test
	public void testEndLineComment() throws BadLocationException {
		fDocument.set("private string foo= \"foobar\" // comment\r\n");
		verifySemicolonPosition(12, 28);
	}

	@Test
	public void testFakeEndLineComment() throws BadLocationException {
		fDocument.set("private string foo= \"foobar\" /* comment */   \r\n");
		verifySemicolonPosition(12, 28);
	}

	@Test
	public void testBlockComment() throws BadLocationException {
		fDocument.set("doStuff(arg1 /* comment */, args)  ");
		verifySemicolonPosition(5, 33);
	}

	@Test
	public void testMultiLineComment() throws BadLocationException {
		fDocument.set("private string foo= \"foobar\" /* comment1 \r\n comment2 */");
		verifySemicolonPosition(12, 28);
	}

	@Test
	public void testEndLineCommentWithFor() throws BadLocationException {
		fDocument.set("for (int i= 0; i < 2; i++) // comment");
		verifySemicolonPosition(13, -1);
	}

	@Test
	public void testFakeEndLineCommentWithFor() throws BadLocationException {
		fDocument.set("for (int i= 0; i < 2; i++) /* comment */  ");
		verifySemicolonPosition(13, -1);
	}

	@Test
	public void testMultiLineCommentWithFor() throws BadLocationException {
		fDocument.set("for (int i= 0; i < 2; i++) /* comment\r\n comment2 */  ");
		verifySemicolonPosition(13, -1);
	}

	@Test
	public void testSemicolonInEmptyMethodBody() throws BadLocationException {
		fDocument.set("void foobar(int param) {}");
		verifySemicolonPosition(24, 24);
	}

	@Test
	public void testSemicolonInString() throws BadLocationException {
		fDocument.set("  new Thread(\"\")");
		verifySemicolonPosition(14, -1);
	}

	/* brace tests */

	@Test
	public void testBraceClassDef() throws BadLocationException {
		fDocument.set("public static final class Main ");
		verifyBracePosition(31, 31);
	}

	@Test
	public void testBraceMethodDef() throws BadLocationException {
		fDocument.set("void bla(int p1, int p2) // comment");
		verifyBracePosition(8, 24);
	}

	@Test
	public void testBraceMethodDef2() throws BadLocationException {
		fDocument.set("void bla(int p1, int p2) // comment");
		verifyBracePosition(12, 24);
	}

	@Test
	public void testBraceIf() throws BadLocationException {
		fDocument.set("if (condition && condition()) // comment");
		verifyBracePosition(12, 29);
	}

	@Test
	public void testBraceIf2() throws BadLocationException {
		fDocument.set("if (value == expected) // comment");
		verifyBracePosition(12, 22);
	}

	@Test
	public void testBraceMethodCall() throws BadLocationException {
		fDocument.set("call(param1, param2) // comment");
		verifyBracePosition(12, 12);
	}

	@Test
	public void testBraceElse() throws BadLocationException {
		fDocument.set(" } else somecode();");
		verifyBracePosition(4, 7);
	}

	@Test
	public void testBraceArray() throws BadLocationException {
		fDocument.set("int[] arr= bla, bli, blu;");
		verifyBracePosition(10, 10);
		verifyBracePosition(11, 11);
	}

	@Test
	public void testBraceArray2() throws BadLocationException {
		fDocument.set("arr= bla, bli, blu;");
		verifyBracePosition(4, 4);
		verifyBracePosition(5, 5);
	}

	@Test
	public void testBraceAnonymousClassDef() throws BadLocationException {
		fDocument.set("this.addListener(blu, new Listener(), bla)");
		verifyBracePosition(34, 36);
	}

	@Test
	public void testBraceAnonymousClassDef2() throws BadLocationException {
		fDocument.set("this.addListener(blu, \"new\", Listener(), bla)");
		verifyBracePosition(41, 41);
	}

	@Test
	public void testBraceIfAnonymousClassDef() throws BadLocationException {
		fDocument.set("  if addListener(blu, new Listener(), bla)");
		verifyBracePosition(34, 36);
		verifyBracePosition(37, 42);
	}

	@Test
	public void testBraceIfAnonymousClassDef2() throws BadLocationException {
		fDocument.set("  if addListener(blu, \"new\", Listener(), bla)");
		verifyBracePosition(20, 45);
		verifyBracePosition(39, 45);
	}

	@Test
	public void testBraceAnonymousClassInstantiation() throws BadLocationException {
		fDocument.set("  Object object=new Object();");
		verifyBracePosition(27, 28);
		verifyBracePosition(28, 28);
	}

	@Test
	public void testBraceLambdaBlock() throws BadLocationException {
		fDocument.set("  new Thread(() -> );");
		verifyBracePosition(18, 18);
		verifyBracePosition(19, 19);
		verifyBracePosition(20, 20);
	}

	@Test
	public void testBraceInString() throws BadLocationException {
		fDocument.set("  new Thread(\"\")");
		verifyBracePosition(14, 14);
	}

	@Test
	public void testBraceInString2() throws BadLocationException {
		fDocument.set("  System.out.write(\"((?\\\"{\\\\\\\"\\\\\\\":\\\"+()+\\\",\\\":\\\"ss\\\").());try {\");");
		verifyBracePosition(50, 50);
	}
}
