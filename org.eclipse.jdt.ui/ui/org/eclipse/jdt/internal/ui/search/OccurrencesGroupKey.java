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

import org.eclipse.jdt.core.IJavaElement;

public class OccurrencesGroupKey extends JavaElementLine {
	private boolean fIsWriteAccess;
	private boolean fIsVariable;
	
	/**
	 * @param element either an ICompilationUnit or an IClassFile
	 */
	public OccurrencesGroupKey(IJavaElement element, int line, String lineContents, boolean isWriteAccess, boolean isVariable) {
		super(element, line, lineContents);
		fIsWriteAccess= isWriteAccess;
		fIsVariable= isVariable;
	}

	public boolean isVariable() {
		return fIsVariable;
	}

	public boolean isWriteAccess() {
		return fIsWriteAccess;
	}

	public void setWriteAccess(boolean isWriteAccess) {
		fIsWriteAccess= isWriteAccess;
	}
}
