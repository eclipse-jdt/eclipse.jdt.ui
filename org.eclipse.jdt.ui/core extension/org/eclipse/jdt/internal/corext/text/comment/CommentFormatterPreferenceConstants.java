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

package org.eclipse.jdt.internal.corext.text.comment;

/**
 * The CommentFormatter's preference keys.
 * 
 * @since 3.1
 */
public class CommentFormatterPreferenceConstants {

	/**
	 * A named preference that controls whether code snippets are formatted
	 * in Javadoc comments.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_FORMATSOURCE= "comment_format_source_code"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls whether description of Javadoc
	 * parameters are indented.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION= "comment_indent_parameter_description"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether the header comment of
	 * a Java source file is formatted.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_FORMATHEADER= "comment_format_header"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether Javadoc root tags
	 * are indented.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_INDENTROOTTAGS= "comment_indent_root_tags"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls whether Javadoc comments
	 * are formatted by the content formatter.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_FORMAT= "comment_format_comments"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether a new line is inserted
	 * after Javadoc root tag parameters.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_NEWLINEFORPARAMETER= "comment_new_line_for_parameter"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether an empty line is inserted before
	 * the Javadoc root tag block.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_SEPARATEROOTTAGS= "comment_separate_root_tags"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether blank lines are cleared during formatting.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_CLEARBLANKLINES= "comment_clear_blank_lines"; //$NON-NLS-1$

	/**
	 * A named preference that controls the line length of comments.
	 * <p>
	 * Value is of type <code>Integer</code>. The value must be at least 4 for reasonable formatting.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_LINELENGTH= "comment_line_length"; //$NON-NLS-1$

	/**
	 * A named preference that controls whether HTML tags are formatted.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */	
	public final static String FORMATTER_COMMENT_FORMATHTML= "comment_format_html"; //$NON-NLS-1$
}
