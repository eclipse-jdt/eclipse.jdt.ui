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

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public class ReplaceCorrectionProposal extends CUCorrectionProposal {
	
	private String fReplacementString;
	private int fOffset;
	private int fLength;

	public ReplaceCorrectionProposal(String label, ProblemPosition problemPos, String replacementString, int relevance) throws CoreException {
		this(label, problemPos.getCompilationUnit(), problemPos.getOffset(), problemPos.getLength(), replacementString, relevance);
	}
	
	public ReplaceCorrectionProposal(String label, ICompilationUnit cu, int offset, int length, String replacementString, int relevance) throws CoreException {
		super(label, cu, relevance);
		fReplacementString= replacementString;
		fOffset= offset;
		fLength= length;
	}	

	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		TextEdit edit= SimpleTextEdit.createReplace(fOffset, fLength, fReplacementString);
		changeElement.getEdit().add(edit);
	}
	
}
