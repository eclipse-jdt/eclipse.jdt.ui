/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ITypeRoot;

public class OccurrencesGroupKey extends JavaElementLine {
	private boolean fIsWriteAccess;
	private boolean fIsVariable;
	
	/**
	 * Create a new occurrences group key.
	 * 
	 * @param element either an ICompilationUnit or an IClassFile
	 * @param lineNumber the line number
	 * @param lineStartOffset the start offset of the line
	 * @param isWriteAccess <code>true</code> if it groups writable occurrences
	 * @param isVariable <code>true</code> if it groups variable occurrences
	 * @throws CoreException thrown when accessing of the buffer failed
	 */
	public OccurrencesGroupKey(ITypeRoot element, int lineNumber, int lineStartOffset, boolean isWriteAccess, boolean isVariable) throws CoreException {
		super(element, lineNumber, lineStartOffset);
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
