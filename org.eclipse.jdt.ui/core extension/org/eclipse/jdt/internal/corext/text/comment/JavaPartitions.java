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
 * Java partitions and their mapping from comment snippet kinds.
 * 
 * @see org.eclipse.jdt.core.formatter.CodeFormatter about comment/code snippet kinds. 
 * @since 3.1
 */
public class JavaPartitions {
	
	public final static String JAVA_SINGLE_LINE_COMMENT= "__java_singleline_comment"; //$NON-NLS-1$
	public final static String JAVA_MULTI_LINE_COMMENT= "__java_multiline_comment"; //$NON-NLS-1$
	public final static String JAVA_DOC= "__java_javadoc"; //$NON-NLS-1$

	// TODO: move to CodeFormatter
	public static final int K_SINGLE_LINE_COMMENT= 0x10;
	public static final int K_MULTI_LINE_COMMENT= 0x20;
	public static final int K_JAVA_DOC= 0x40;

	public static String getPartitionType(int kind) {
		switch (kind) {
			case K_SINGLE_LINE_COMMENT:
				return JAVA_SINGLE_LINE_COMMENT;
			case K_MULTI_LINE_COMMENT:
				return JAVA_MULTI_LINE_COMMENT;
			case K_JAVA_DOC:
				return JAVA_DOC;
			default:
				return null;
		}
	}
}
