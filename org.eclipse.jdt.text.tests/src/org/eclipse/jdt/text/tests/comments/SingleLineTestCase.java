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

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

public class SingleLineTestCase extends CommentTestCase {

	protected static final String PREFIX= "// ";

	public static Test suite() {
		return new TestSuite(SingleLineTestCase.class);
	}

	public SingleLineTestCase(String name) {
		super(name);
	}

	/*
	 * @see org.eclipse.jdt.text.tests.comments.CommentTestCase#getCommentType()
	 */
	protected String getCommentType() {
		return IJavaPartitions.JAVA_SINGLE_LINE_COMMENT;
	}

	public void testCommentWrapping5() {
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		String prefix= "public class Test {" + DELIMITER + "	int test; // test test test test test test test test test test test test";
		String inputInfix= " ";
		String expectedInfix= DELIMITER + "\t\t\t\t" + PREFIX;
		String suffix= "test" + DELIMITER + "}" + DELIMITER;
		String input= prefix + inputInfix + suffix;
		int offset= input.indexOf("//");
		assertEquals(prefix + expectedInfix + suffix, testFormat(input, offset, input.indexOf(DELIMITER, offset) + DELIMITER.length() - offset));
	}

	public void testHeaderComment1() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER, IPreferenceStore.FALSE);
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "12"); //$NON-NLS-1$
		assertEquals(PREFIX + "test test" + DELIMITER + PREFIX + "test test" + DELIMITER + PREFIX + "test test" + DELIMITER, testFormat("//test\t\t\t\ttest" + DELIMITER + PREFIX + "test test test test")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	public void testHeaderComment2() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER, IPreferenceStore.FALSE);
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "24"); //$NON-NLS-1$
		assertEquals(PREFIX + "test test test test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//test\t\t\t" + DELIMITER + PREFIX + "test test test test" + DELIMITER)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public void testIndentedComment1() {
		String prefix= "public class Test {" + DELIMITER + "\t";
		String comment= PREFIX + "test" + DELIMITER;
		String postfix= "}" + DELIMITER;
		String string= prefix + comment + postfix;
		assertEquals(string, testFormat(string, prefix.length(), comment.length()));
	}
	
	public void testIndentedComment2() {
		String prefix= "public class Test {" + DELIMITER + "\tpublic void test() {" + DELIMITER + "\t\t";
		String comment= PREFIX + "test" + DELIMITER;
		String postfix= "\t}" + DELIMITER + "}" + DELIMITER;
		String string= prefix + comment + postfix;
		assertEquals(string, testFormat(string, prefix.length(), comment.length()));
	}
	
	public void testIndentedComment3() {
		String prefix= "public class Test {" + DELIMITER + "\tpublic void test() {" + DELIMITER + "\t\tif (true) {" + DELIMITER + "\t\t\t";
		String comment= PREFIX + "test" + DELIMITER;
		String postfix= "\t\t}" + DELIMITER + "\t}" + DELIMITER + "}" + DELIMITER;
		String string= prefix + comment + postfix;
		assertEquals(string, testFormat(string, prefix.length(), comment.length()));
	}
	
	public void testNoChange1() {
		String content= PREFIX;
		assertEquals(content, testFormat(content));
	}
	
	public void testNoFormat1() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_FORMAT, "false");
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "1");
		String content= PREFIX + "test test";
		assertEquals(content, testFormat(content));
	}
}
