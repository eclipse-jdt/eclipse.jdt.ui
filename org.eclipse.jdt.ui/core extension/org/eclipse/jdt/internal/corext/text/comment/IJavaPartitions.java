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
 * Java partitions, these must be equal to those in {@link org.eclipse.jdt.internal.ui.text.IJavaPartitions}.
 * TODO: remove this dependency
 * 
 * @since 3.1
 */
public interface IJavaPartitions {
	
	public final static String JAVA_SINGLE_LINE_COMMENT= "__java_singleline_comment"; //$NON-NLS-1$
	public final static String JAVA_MULTI_LINE_COMMENT= "__java_multiline_comment"; //$NON-NLS-1$
	public final static String JAVA_DOC= "__java_javadoc"; //$NON-NLS-1$
}
