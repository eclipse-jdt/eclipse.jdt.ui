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

import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

public class MultiLineTestCase extends CommentTestCase {

	private static final String INFIX= " * ";

	private static final String POSTFIX= " */";

	private static final String PREFIX= "/* ";

	public static Test suite() {
		return new TestSuite(MultiLineTestCase.class);
	}

	public MultiLineTestCase(String name) {
		super(name);
	}

	/*
	 * @see org.eclipse.jdt.text.tests.comments.CommentTestCase#getCommentType()
	 */
	protected String getCommentType() {
		return IJavaPartitions.JAVA_MULTI_LINE_COMMENT;
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
