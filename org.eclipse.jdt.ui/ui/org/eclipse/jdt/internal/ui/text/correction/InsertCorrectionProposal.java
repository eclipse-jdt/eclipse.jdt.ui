/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;


public class InsertCorrectionProposal extends CUCorrectionProposal {
	
	private String fInsertionString;
	private int  fOffset;

	public InsertCorrectionProposal(String label, ICompilationUnit cu, int offset, String insertString, int relevance) throws CoreException {
		super(label, cu, relevance);
		fInsertionString= insertString;
		fOffset= offset;
	}

	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		TextEdit edit= SimpleTextEdit.createInsert(fOffset, fInsertionString);
		changeElement.getEdit().add(edit);
	}
	
	public static int moveBack(int offset, int start, String ignoreCharacters, ICompilationUnit cu) {
		try {
			IBuffer buf= cu.getBuffer();
			while (offset >= start) {
				if (ignoreCharacters.indexOf(buf.getChar(offset - 1)) == -1) { //$NON-NLS-1$
					return offset;
				}
				offset--;
			}
		} catch(JavaModelException e) {
		}
		return start;
	}

}
