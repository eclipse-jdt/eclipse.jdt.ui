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

public class JavaElementLine {
	private IJavaElement fElement;
	private int fLine;
	private String fLineContents;
	
	/**
	 * @param element either an ICompilationUnit or an IClassFile
	 */
	public JavaElementLine(IJavaElement element, int line, String lineContents) {
		fElement= element;
		fLine= line;
		fLineContents= lineContents;
	}
	
	public IJavaElement getJavaElement() {
		return fElement;
	}
	
	public int getLine() {
		return fLine;
	}
	
	public String getLineContents() {
		return fLineContents;
	}
}
