package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.dom.ASTRewriteAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.changes.enhanced.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.TextBuffer;

/**
  */
public class ASTRewriteCorrectionProposal extends CUCorrectionProposalEnhanced {

	private CompilationUnit fAstRoot;

	public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu, CompilationUnit astRoot, int relevance, Image image) throws CoreException {
		super(name, cu, relevance, image);
		fAstRoot= astRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#addEdits(CompilationUnitChange)
	 */
	protected void addEdits(CompilationUnitChange change) throws CoreException {
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(change.getFile());
			
			ASTRewriteAnalyzer analyser= new ASTRewriteAnalyzer(buffer, change); 
			fAstRoot.accept(analyser);
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
		}		

	}

}
