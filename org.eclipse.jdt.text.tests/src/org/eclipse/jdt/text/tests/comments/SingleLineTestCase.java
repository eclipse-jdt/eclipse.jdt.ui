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

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.text.comment.SingleCommentLine;

import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

public class SingleLineTestCase extends CommentTestCase {

	protected static final String PREFIX= SingleCommentLine.SINGLE_COMMENT_PREFIX;

	public static Test suite() {
		return new TestSuite(SingleLineTestCase.class);
	}

	public SingleLineTestCase(String name) {
		super(name);
	}

	protected String getCommentType() {
		return IJavaPartitions.JAVA_SINGLE_LINE_COMMENT;
	}

	public void testClearBlankLines1() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "5"); //$NON-NLS-1$
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES, IPreferenceStore.FALSE);
		assertEquals(PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//test\ttest" + DELIMITER + "//" + DELIMITER + "//\t\ttest")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	}

	public void testClearBlankLines2() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "5"); //$NON-NLS-1$
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES, IPreferenceStore.FALSE);
		assertEquals(PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//test\t\ttest" + DELIMITER + PREFIX + DELIMITER + "//\t\ttest")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}

	public void testClearBlankLines3() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "5"); //$NON-NLS-1$
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES, IPreferenceStore.FALSE);
		assertEquals(PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//test\ttest" + DELIMITER + "//" + DELIMITER + PREFIX + "test\ttest")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	}

	public void testCommentBegin1() {
		assertEquals(PREFIX + "test" + DELIMITER, testFormat("//test")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentBegin2() {
		assertEquals(PREFIX + "test" + DELIMITER, testFormat(PREFIX + "test")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentBegin3() {
		assertEquals(PREFIX + "test" + DELIMITER, testFormat("//\t\ttest " + DELIMITER)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentDelimiter1() {
		assertEquals(PREFIX + "test" + DELIMITER, testFormat("//\t\ttest " + DELIMITER + DELIMITER)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentDelimiter2() {
		assertEquals(PREFIX + "test" + DELIMITER, testFormat(PREFIX + "test " + DELIMITER + DELIMITER + DELIMITER)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentNls1() {
		assertEquals("//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$", testFormat("//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentNls2() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "1"); //$NON-NLS-1$
		assertEquals("//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$", testFormat("//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentNls3() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "5"); //$NON-NLS-1$
		assertEquals("//$NON-NLS-1", testFormat("//$NON-NLS-1")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentNls4() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "5"); //$NON-NLS-1$
		assertEquals("//$NON-NLS-4", testFormat("//$NON-NLS-4")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentNls5() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "-2"); //$NON-NLS-1$
		assertEquals("//$NON-NLS-15$", testFormat("//$NON-NLS-15$")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentSpace1() {
		assertEquals(PREFIX + "test test" + DELIMITER, testFormat("//test\t \t test")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentSpace2() {
		assertEquals(PREFIX + "test test" + DELIMITER, testFormat("//test test")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentSpace3() {
		assertEquals(PREFIX + "test test" + DELIMITER, testFormat(PREFIX + "test \t    \t test")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testCommentWrapping1() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "5"); //$NON-NLS-1$
		assertEquals(PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//test\ttest")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testCommentWrapping2() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "1"); //$NON-NLS-1$
		assertEquals(PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//test\ttest")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testCommentWrapping3() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "32"); //$NON-NLS-1$
		assertEquals(PREFIX + "test test" + DELIMITER, testFormat("//test\ttest")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testCommentWrapping4() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "32"); //$NON-NLS-1$
		assertEquals(PREFIX + "test test" + DELIMITER, testFormat("//test\ttest" + DELIMITER)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

	public void testIllegalLineLength1() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "1"); //$NON-NLS-1$
		assertEquals(PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//test\ttest")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testIllegalLineLength2() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "-16"); //$NON-NLS-1$
		assertEquals(PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat(PREFIX + "\t\t test\ttest")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testMultipleComments1() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "5"); //$NON-NLS-1$
		assertEquals(PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//test test" + DELIMITER + PREFIX + "test test test test")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	}

	public void testMultipleComments2() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "12"); //$NON-NLS-1$
		assertEquals(PREFIX + "test test" + DELIMITER + PREFIX + "test test" + DELIMITER + PREFIX + "test test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//test test\ttest" + DELIMITER + PREFIX + DELIMITER + PREFIX + "test test test test")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}

	public void testMultipleComments3() {
		setUserOption(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, "11"); //$NON-NLS-1$
		assertEquals(PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER + PREFIX + "test" + DELIMITER, testFormat("//   test\t\t\ttest\ttest" + DELIMITER + PREFIX + "test test test test")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
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
}
