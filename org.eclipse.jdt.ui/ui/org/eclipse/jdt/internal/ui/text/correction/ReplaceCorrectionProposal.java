/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;



public class ReplaceCorrectionProposal extends CUCorrectionProposal {
	
	private String fReplacementString;

	public ReplaceCorrectionProposal(ProblemPosition problemPos, String label, String replacementString) throws CoreException {
		super(label, problemPos);
		fReplacementString= replacementString;
	}

	/*
	 * @see JavaCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange changeElement) throws CoreException {
		ProblemPosition problemPos= getProblemPosition();
		
		TextEdit edit= SimpleTextEdit.createReplace(problemPos.getOffset(), problemPos.getLength(), fReplacementString);
		changeElement.addTextEdit(changeElement.getName(), edit);
	}
	
}
