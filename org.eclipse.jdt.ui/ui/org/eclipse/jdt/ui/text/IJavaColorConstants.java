/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.text;

/**
 * Color keys used for syntax highlighting Java 
 * code and JavaDoc compliant comments. 
 * A <code>IColorManager</code> is responsible for mapping 
 * concrete colors to these keys.
 * <p>
 * This interface declares static final fields only; it is not intended to be 
 * implemented.
 * </p>
 *
 * @see IColorManager
 */
public interface IJavaColorConstants {
	
	/** The prefix all color constants start with */
	String PREFIX= "java_"; //$NON-NLS-1$
	
	/** The color key for multi-line comments in Java code. */
	String JAVA_MULTI_LINE_COMMENT= "java_multi_line_comment"; //$NON-NLS-1$
	/** The color key for single-line comments in Java code. */
	String JAVA_SINGLE_LINE_COMMENT= "java_single_line_comment"; //$NON-NLS-1$
	/** The color key for Java keywords in Java code. */
	String JAVA_KEYWORD= "java_keyword"; //$NON-NLS-1$
	/** The color key for string and character literals in Java code. */
	String JAVA_STRING= "java_string"; //$NON-NLS-1$
	/** The color key for everthing in Java code for which no other color is specified. */
	String JAVA_DEFAULT= "java_default"; //$NON-NLS-1$
	/** 
	 * The color key for the Java built-in types such as int and char in Java code.
	 * @deprecated no longer used, use <code>JAVA_KEYWORD</code> instead
	 */
	String JAVA_TYPE= "java_type"; //$NON-NLS-1$
	
	
	/** The color key for JavaDoc keywords (<code>@foo</code>) in JavaDoc comments. */
	String JAVADOC_KEYWORD= "java_doc_keyword"; //$NON-NLS-1$
	/** The color key for HTML tags (<code>&lt;foo&gt;</code>) in JavaDoc comments. */
	String JAVADOC_TAG= "java_doc_tag"; //$NON-NLS-1$
	/** The color key for JavaDoc links (<code>{foo}</code>) in JavaDoc comments. */
	String JAVADOC_LINK= "java_doc_link"; //$NON-NLS-1$
	/** The color key for everthing in JavaDoc comments for which no other color is specified. */
	String JAVADOC_DEFAULT= "java_doc_default"; //$NON-NLS-1$
}