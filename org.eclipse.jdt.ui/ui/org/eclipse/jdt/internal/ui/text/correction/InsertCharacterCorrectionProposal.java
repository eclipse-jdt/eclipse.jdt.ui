/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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


	public InsertCharacterCorrectionProposal(ICompilationUnit cu, ProblemPosition problemPos, String label, String insertString, boolean atBeginning) throws CoreException {
		super(label, cu, problemPos);
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
				if ("\n\r".indexOf(buf.getChar(offset - 1)) == -1) {
					return offset;
				}
				offset--;
			}
		} catch(JavaModelException e) {
		}
		return start;
	}

}
