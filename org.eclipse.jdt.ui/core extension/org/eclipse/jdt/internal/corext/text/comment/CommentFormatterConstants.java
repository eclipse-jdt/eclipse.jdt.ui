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
 * TODO: merge with CodeFormatter
 * @since 3.1
 */
public class CommentFormatterConstants {

	/**
	 * Kind used to format single-line comments
	 */
	public static final int K_SINGLE_LINE_COMMENT= 0x10;
	/**
	 * Kind used to format multi-line comments
	 */
	public static final int K_MULTI_LINE_COMMENT= 0x20;
	/**
	 * Kind used to format a Javadoc comments
	 */
	public static final int K_JAVA_DOC= 0x40;

	/**
	 * This class is not intended for instantiation.
	 */
	private CommentFormatterConstants() {
		// Not for instantiation
	}
}
