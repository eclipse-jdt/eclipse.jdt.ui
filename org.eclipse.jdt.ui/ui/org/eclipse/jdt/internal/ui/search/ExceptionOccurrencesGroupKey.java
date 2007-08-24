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

public class ExceptionOccurrencesGroupKey extends JavaElementLine {
	private boolean fIsException;
	
	/**
	 * @param element either an ICompilationUnit or an IClassFile
	 * @param lineNumber the line number
	 * @param lineStartOffset the line start offset
	 * @param isException specifies if the occurrence represents the thrown exception declaration
	 * @throws CoreException thrown when accessing of the buffer failed
	 */
	public ExceptionOccurrencesGroupKey(ITypeRoot element, int lineNumber, int lineStartOffset, boolean isException) throws CoreException {
		super(element, lineNumber, lineStartOffset);
		fIsException= isException;
	}

	public boolean isException() {
		return fIsException;
	}

	public void setException(boolean isException) {
		fIsException= isException;
	}
}
