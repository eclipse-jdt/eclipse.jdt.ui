/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.comments;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.text.IJavaPartitions;


public class JavaDocTestCase extends CommentTestCase {

	protected static final String INFIX= " * ";

	protected static final String POSTFIX= " */";

	protected static final String PREFIX= "/**";
	
	public static Test suite() {
		return new TestSuite(JavaDocTestCase.class);
	}

	public JavaDocTestCase(String name) {
		super(name);
	}

	/*
	 * @see org.eclipse.jdt.text.tests.comments.CommentTestCase#getCommentType()
	 */
	protected String getCommentType() {
		return IJavaPartitions.JAVA_DOC;
	}

	public void testMultiLineCommentIndentTabs1() {
		String prefix= "public class Test {" + DELIMITER + "\t\t"; //$NON-NLS-1$ //$NON-NLS-2$
		String content= PREFIX + DELIMITER + "\t\t\t" + INFIX + "test test" + DELIMITER + "\t\t\t\t" + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String postfix= DELIMITER + "}"; //$NON-NLS-1$
		String expected= PREFIX + DELIMITER + "\t\t" + INFIX + "test test" + DELIMITER + "\t\t" + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		assertEquals(prefix + expected + postfix, testFormat(prefix + content + postfix, prefix.length(), content.length()));
	}
	
	/**
	 * [formatting] Comments formatter inserts tabs when it should use spaces
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=47491
	 */
	public void testMultiLineCommentIndentSpaces1() {
		String prefix= "public class Test {" + DELIMITER + "\t"; //$NON-NLS-1$ //$NON-NLS-2$
		String content= PREFIX + DELIMITER + "\t\t" + INFIX + "test test" + DELIMITER + "        " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String postfix= DELIMITER + "}"; //$NON-NLS-1$
		String expected= PREFIX + DELIMITER + "   " + INFIX + "test test" + DELIMITER + "   " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "3"); //$NON-NLS-1$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "3"); //$NON-NLS-1$ // this is really the visual tab length setting
		assertEquals(prefix + expected + postfix, testFormat(prefix + content + postfix, prefix.length(), content.length()));
	}
	
	public void testMultiLineCommentIndentSpaces2() {
		String prefix= "public class Test {" + DELIMITER + "    "; //$NON-NLS-1$ //$NON-NLS-2$
		String content= PREFIX + DELIMITER + "\t\t" + INFIX + "test test" + DELIMITER + "        " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String postfix= DELIMITER + "}"; //$NON-NLS-1$
		String expected= PREFIX + DELIMITER + "      " + INFIX + "test test" + DELIMITER + "      " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "3"); //$NON-NLS-1$
		assertEquals(prefix + expected + postfix, testFormat(prefix + content + postfix, prefix.length(), content.length()));
	}
	
	public void testMultiLineCommentIndentSpaces3() {
		String prefix= "public class Test {" + DELIMITER + "  \t  "; //$NON-NLS-1$ //$NON-NLS-2$
		String content= PREFIX + DELIMITER + "\t\t" + INFIX + "test test" + DELIMITER + "        " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String postfix= DELIMITER + "}"; //$NON-NLS-1$
		String expected= PREFIX + DELIMITER + "      " + INFIX + "test test" + DELIMITER + "      " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "3"); //$NON-NLS-1$
		assertEquals(prefix + expected + postfix, testFormat(prefix + content + postfix, prefix.length(), content.length()));
	}
	
	public void testMultiLineCommentIndentSpaces4() {
		String prefix= "public class Test {" + DELIMITER + "   \t   "; //$NON-NLS-1$ //$NON-NLS-2$
		String content= PREFIX + DELIMITER + "\t\t" + INFIX + "test test" + DELIMITER + "        " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String postfix= DELIMITER + "}"; //$NON-NLS-1$
		String expected= PREFIX + DELIMITER + "         " + INFIX + "test test" + DELIMITER + "         " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "3"); //$NON-NLS-1$
		assertEquals(prefix + expected + postfix, testFormat(prefix + content + postfix, prefix.length(), content.length()));
	}
	
	public void testMultiLineCommentIndentMixed1() {
		String prefix= "public class Test {" + DELIMITER + "     "; //$NON-NLS-1$ //$NON-NLS-2$
		String content= PREFIX + DELIMITER + "\t\t" + INFIX + "test test" + DELIMITER + "        " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String postfix= DELIMITER + "}"; //$NON-NLS-1$
		String expected= PREFIX + DELIMITER + "\t" + INFIX + "test test" + DELIMITER + "\t" + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, DefaultCodeFormatterConstants.MIXED);
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "6"); //$NON-NLS-1$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "3"); //$NON-NLS-1$
		assertEquals(prefix + expected + postfix, testFormat(prefix + content + postfix, prefix.length(), content.length()));
	}
	
	public void testMultiLineCommentIndentMixed2() {
		String prefix= "public class Test {" + DELIMITER + "\t "; //$NON-NLS-1$ //$NON-NLS-2$
		String content= PREFIX + DELIMITER + "\t\t" + INFIX + "test test" + DELIMITER + "        " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String postfix= DELIMITER + "}"; //$NON-NLS-1$
		String expected= PREFIX + DELIMITER + "\t   " + INFIX + "test test" + DELIMITER + "\t   " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, DefaultCodeFormatterConstants.MIXED);
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "6"); //$NON-NLS-1$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "3"); //$NON-NLS-1$
		assertEquals(prefix + expected + postfix, testFormat(prefix + content + postfix, prefix.length(), content.length()));
	}
	
	public void testMultiLineCommentIndentMixed3() {
		String prefix= "public class Test {" + DELIMITER + "  "; //$NON-NLS-1$ //$NON-NLS-2$
		String content= PREFIX + DELIMITER + "\t\t" + INFIX + "test test" + DELIMITER + "        " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String postfix= DELIMITER + "}"; //$NON-NLS-1$
		String expected= PREFIX + DELIMITER + "   " + INFIX + "test test" + DELIMITER + "   " + POSTFIX;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, DefaultCodeFormatterConstants.MIXED);
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "6"); //$NON-NLS-1$
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "3"); //$NON-NLS-1$
		assertEquals(prefix + expected + postfix, testFormat(prefix + content + postfix, prefix.length(), content.length()));
	}
	
	public void testNoChange1() {
		String content= PREFIX + DELIMITER + POSTFIX;
		assertEquals(content, testFormat(content));
	}
	
	public void testNoFormat1() {
		setUserOption(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT, "false");
		String content= PREFIX + DELIMITER + INFIX + "test" + DELIMITER + INFIX + "test" + DELIMITER + POSTFIX;
		assertEquals(content, testFormat(content));
	}
}
