/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.comments;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.text.comment.MultiCommentLine;

import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

public class MultiLineTestCase extends CommentTestCase {

	protected static final String INFIX= MultiCommentLine.MULTI_COMMENT_CONTENT_PREFIX;

	protected static final String POSTFIX= MultiCommentLine.MULTI_COMMENT_END_PREFIX;

	protected static final String PREFIX= MultiCommentLine.MULTI_COMMENT_START_PREFIX;

	public static Test suite() {
		return new TestSuite(MultiLineTestCase.class);
	}

	public MultiLineTestCase(String name) {
		super(name);
	}

	protected String getCommentType() {
		return IJavaPartitions.JAVA_MULTI_LINE_COMMENT;
	}

	public void testSingleLineComment1() {
		assertEquals("/*" + DELIMITER + INFIX + "test" + DELIMITER + POSTFIX, testFormat("/*\t\t" + DELIMITER + "*\t test*/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public void testSingleLineComment2() {
		assertEquals("/*" + DELIMITER + INFIX + "test" + DELIMITER + POSTFIX, testFormat(PREFIX + "test" + DELIMITER + "\t" + POSTFIX)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public void testSingleLineComment3() {
		assertEquals("/*" + DELIMITER + INFIX + "test" + DELIMITER + POSTFIX, testFormat(PREFIX + DELIMITER + "* test\t*/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testSingleLineComment4() {
		assertEquals("/*" + DELIMITER + INFIX + "test" + DELIMITER + POSTFIX, testFormat("/*test" + DELIMITER + POSTFIX)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testSingleLineCommentSpace1() {
		assertEquals(PREFIX + "test" + POSTFIX, testFormat("/*test*/")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testSingleLineCommentSpace2() {
		assertEquals(PREFIX + "test" + POSTFIX, testFormat("/*test" + POSTFIX)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testSingleLineCommentSpace3() {
		assertEquals(PREFIX + "test" + POSTFIX, testFormat(PREFIX + "test*/")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testSingleLineCommentSpace4() {
		assertEquals(PREFIX + "test test" + POSTFIX, testFormat("/* test   test*/")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testSingleLineCommentTabs1() {
		assertEquals(PREFIX + "test test" + POSTFIX, testFormat("/*\ttest\ttest" + POSTFIX)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testSingleLineCommentTabs2() {
		assertEquals(PREFIX + "test test" + POSTFIX, testFormat("/*\ttest\ttest*/")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * [formatting] formatter removes last line with block comments
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51654
	 */
	public void testMultiLineCommentAsterisk1() {
		// test3 (currently) forces the comment formatter to actually do something, it wouldn't do anything otherwise.
		String input= PREFIX + INFIX + "test1" + DELIMITER + "test2" + INFIX + DELIMITER + "test3" + DELIMITER + "test4" + POSTFIX; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		String result= testFormat(input);
		assertTrue(result.indexOf("test1") != -1); //$NON-NLS-1$
		assertTrue(result.indexOf("test2") != -1); //$NON-NLS-1$
		assertTrue(result.indexOf("test3") != -1); //$NON-NLS-1$
		assertTrue(result.indexOf("test4") != -1); //$NON-NLS-1$
	}
	
	public void testMultiLineCommentHeader1() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER, "false");
		String prefix= PREFIX.trim() + DELIMITER + INFIX + "header" + DELIMITER + INFIX + "comment" + DELIMITER + POSTFIX + DELIMITER;
		String inputInfix = JavaDocTestCase.PREFIX + DELIMITER + INFIX + "class" + DELIMITER + INFIX + "comment" + DELIMITER + POSTFIX + DELIMITER;
		String expectedInfix = JavaDocTestCase.PREFIX + DELIMITER + INFIX + "class comment" + DELIMITER + POSTFIX + DELIMITER;
		String postfix= "public class Test {" + DELIMITER + "}" + DELIMITER;
		
		String output= testFormat(prefix + inputInfix + postfix, 0, prefix.length() - DELIMITER.length(), IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		int offset= output.lastIndexOf(JavaDocTestCase.PREFIX);
		output= testFormat(output, offset, output.lastIndexOf(POSTFIX) + POSTFIX.length() - offset, IJavaPartitions.JAVA_DOC);
		assertEquals(prefix + expectedInfix + postfix, output);
	}
	
	public void testMultiLineCommentHeader2() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER, "true");
		String inputPrefix= PREFIX.trim() + DELIMITER + INFIX + "header" + DELIMITER + INFIX + "comment" + DELIMITER + POSTFIX + DELIMITER;
		String expectedPrefix= PREFIX.trim() + DELIMITER + INFIX + "header comment" + DELIMITER + POSTFIX + DELIMITER;
		String inputInfix = JavaDocTestCase.PREFIX + DELIMITER + INFIX + "class" + DELIMITER + INFIX + "comment" + DELIMITER + POSTFIX + DELIMITER;
		String expectedInfix = JavaDocTestCase.PREFIX + DELIMITER + INFIX + "class comment" + DELIMITER + POSTFIX + DELIMITER;
		String postfix= "public class Test {" + DELIMITER + "}" + DELIMITER;
		
		String output= testFormat(inputPrefix + inputInfix + postfix, 0, inputPrefix.length() - DELIMITER.length(), IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		int offset= output.lastIndexOf(JavaDocTestCase.PREFIX);
		output= testFormat(output, offset, output.lastIndexOf(POSTFIX) + POSTFIX.length() - offset, IJavaPartitions.JAVA_DOC);
		assertEquals(expectedPrefix + expectedInfix + postfix, output);
	}
	
	public void testNoChange1() {
		String content= PREFIX + DELIMITER + POSTFIX;
		assertEquals(content, testFormat(content));
	}
	
	public void testNoFormat1() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_FORMAT, "false");
		String content= PREFIX + DELIMITER + INFIX + "test" + DELIMITER + INFIX + "test" + DELIMITER + POSTFIX;
		assertEquals(content, testFormat(content));
	}
}
