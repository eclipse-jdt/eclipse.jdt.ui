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

import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.IJavaElement;

/**
 * @author Thomas Mäder
 *
 */
public class OccurrenceMatch extends Match {
	private boolean fIsWriteAccess;
	private boolean fIsVariable;
	private String fText;
	
	public OccurrenceMatch(IJavaElement root, String text, int offset, int length, boolean isWriteAccess, boolean isVariable) {
		super(root, offset, length);
		fText= text;
		fIsVariable= isVariable;
		fIsWriteAccess= isWriteAccess;
	}

	boolean isVariable() {
		return fIsVariable;
	}

	boolean isWriteAccess() {
		return fIsWriteAccess;
	}

	String getText() {
		return fText;
	}
}
