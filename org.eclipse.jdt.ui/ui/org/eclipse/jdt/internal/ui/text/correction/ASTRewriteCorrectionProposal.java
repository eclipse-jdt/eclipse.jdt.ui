package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

/**
  */
public class ASTRewriteCorrectionProposal extends CUCorrectionProposal {

	private ASTRewrite fRewrite;

	public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) throws CoreException {
		super(name, cu, relevance, image);
		fRewrite= rewrite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange change) throws CoreException {
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(change.getFile());
			ArrayList groupDescriptions= new ArrayList(5);
			TextEdit topEdit= fRewrite.rewriteNode(buffer, groupDescriptions);
			change.setEdit(topEdit);
			for (int i= 0; i < groupDescriptions.size(); i++) {
				change.addGroupDescription((GroupDescription) groupDescriptions.get(i));
			}
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
		}		

	}

}
