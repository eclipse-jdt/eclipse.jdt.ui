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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.formatter.IndentManipulation;

import org.eclipse.jdt.ui.JavaUI;

public class JavaElementLine {
	
	private final ITypeRoot fElement;
	private final String fLineContents;
	private final int fLineNumber;
	private final int fLineStartOffset;
	
	/**
	 * @param element either an ICompilationUnit or an IClassFile
	 * @param lineNumber the line number
	 * @param lineStartOffset the start offset of the line
	 * @throws CoreException thrown when accessing of the buffer failed
	 */
	public JavaElementLine(ITypeRoot element, int lineNumber, int lineStartOffset) throws CoreException {
		fElement= element;
		
		IBuffer buffer= element.getBuffer();
		if (buffer == null) {
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, "Element has no buffer: " + element.getElementName())); //$NON-NLS-1$
		}
		
		int length= buffer.getLength();
		int i= lineStartOffset;
		
		char ch= buffer.getChar(i);
		while (lineStartOffset < length && IndentManipulation.isIndentChar(ch)) {
			ch= buffer.getChar(++i);
		}
		fLineStartOffset= i;
		
		StringBuffer buf= new StringBuffer();

		while (i < length && !IndentManipulation.isLineDelimiterChar(ch)) {
			if (Character.isISOControl(ch)) {
				buf.append(' ');
			} else {
				buf.append(ch);
			}
			ch= buffer.getChar(++i);
		}
		fLineContents= buf.toString();
		fLineNumber= lineNumber;
	}

	public IJavaElement getJavaElement() {
		return fElement;
	}
	
	public int getLine() {
		return fLineNumber;
	}
	
	public String getLineContents() {
		return fLineContents;
	}
	
	public int getLineStartOffset() {
		return fLineStartOffset;
	}
}
