package org.eclipse.jdt.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
	
	/** The color key for multi-line comments in Java code. */
	String JAVA_MULTI_LINE_COMMENT= "java_multi_line_comment";
	/** The color key for single-line comments in Java code. */
	String JAVA_SINGLE_LINE_COMMENT= "java_single_line_comment";
	/** The color key for Java keywords in Java code. */
	String JAVA_KEYWORD= "java_keyword";
	/** The color key for the Java built-in types such as int and char in Java code. */
	String JAVA_TYPE= "java_type";
	/** The color key for string and character literals in Java code. */
	String JAVA_STRING= "java_string";
	/** The color key for everthing in Java code for which no other color is specified. */
	String JAVA_DEFAULT= "java_default";
	
	/** The color key for JavaDoc keywords (<code>@foo</code>) in JavaDoc comments. */
	String JAVADOC_KEYWORD= "javadoc_keyword";
	/** The color key for HTML tags (<code>&lt;foo&gt;</code>) in JavaDoc comments. */
	String JAVADOC_TAG= "javadoc_tag";
	/** The color key for JavaDoc links (<code>{foo}</code>) in JavaDoc comments. */
	String JAVADOC_LINK= "javadoc_link";
	/** The color key for everthing in JavaDoc comments for which no other color is specified. */
	String JAVADOC_DEFAULT= "javadoc_default";
}