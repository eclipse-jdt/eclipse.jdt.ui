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
package org.eclipse.jdt.internal.ui.search;

/**
 * Constants used by the Java search
 */
public interface IJavaSearchUIConstants {
	/** Attribute name of the Java Element handle ID in a marker */
	static final String ATT_JE_HANDLE_ID= "org.eclipse.jdt.internal.ui.search.handleID"; //$NON-NLS-1$

	/** Attribute name of Java Element handle ID changed flag in a marker */
	static final String ATT_JE_HANDLE_ID_CHANGED= "org.eclipse.jdt.internal.ui.search.handleIdChanged"; //$NON-NLS-1$

	/** Attribute name for isWorkingCopy property */
	static final String ATT_IS_WORKING_COPY= "org.eclipse.jdt.internal.ui.search.isWorkingCopy"; //$NON-NLS-1$
}
