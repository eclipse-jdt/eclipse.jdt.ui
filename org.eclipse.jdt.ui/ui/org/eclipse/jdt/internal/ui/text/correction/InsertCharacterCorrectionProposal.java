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

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;


public class InsertCharacterCorrectionProposal extends CUCorrectionProposal {
	
	private String fInsertionString;
	private boolean fAtBeginning;


	public InsertCharacterCorrectionProposal(ProblemPosition problemPos, String label, String insertString, boolean atBeginning, int relevance) throws CoreException {
		super(label, problemPos, relevance);
		fInsertionString= insertString;
		fAtBeginning= atBeginning;
	}

	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		ProblemPosition problemPos= getProblemPosition();
		
		int offset= problemPos.getOffset() + problemPos.getLength();
		offset= problemPos.getOffset();
		if (!fAtBeginning) {
			offset+= problemPos.getLength();
			offset= correctOffset(offset, problemPos.getOffset(), getCompilationUnit());
		}
		
		TextEdit edit= SimpleTextEdit.createInsert(offset, fInsertionString);
		changeElement.addTextEdit(getDisplayString(), edit);
	}
	
	private int correctOffset(int offset, int start, ICompilationUnit cu) {
		try {
			IBuffer buf= cu.getBuffer();
			while (offset >= start) {
				if ("\n\r".indexOf(buf.getChar(offset - 1)) == -1) { //$NON-NLS-1$
					return offset;
				}
				offset--;
			}
		} catch(JavaModelException e) {
		}
		return start;
	}

}
