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

package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.jdt.core.ICompilationUnit;


public class StubTypeContext {
	private String fBeforeString;
	private String fAfterString;
	private final ICompilationUnit fCuHandle;
	
	StubTypeContext(ICompilationUnit cuHandle, String beforeString, String afterString) {
		fCuHandle= cuHandle;
		fBeforeString= beforeString;
		fAfterString= afterString;
	}
	
	public ICompilationUnit getCuHandle() {
		return fCuHandle;
	}
	
	public String getBeforeString() {
		return fBeforeString;
	}
	
	public String getAfterString() {
		return fAfterString;
	}
}