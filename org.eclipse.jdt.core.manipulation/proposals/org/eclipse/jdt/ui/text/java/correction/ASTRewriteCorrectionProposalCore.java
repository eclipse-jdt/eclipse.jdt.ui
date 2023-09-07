package org.eclipse.jdt.ui.text.java.correction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.internal.ui.JavaUIStatus;

public class ASTRewriteCorrectionProposalCore extends CUCorrectionProposalCore {
	private ASTRewrite fRewrite;
	private ImportRewrite fImportRewrite;


	public ASTRewriteCorrectionProposalCore(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		super(name, cu, relevance);
		fRewrite = rewrite;
	}

	/**
	 * Returns the import rewrite used for this compilation unit.
	 *
	 * @return the import rewrite or <code>null</code> if no import rewrite has been set
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public ImportRewrite getImportRewrite() {
		return fImportRewrite;
	}

	/**
	 * Sets the import rewrite used for this compilation unit.
	 *
	 * @param rewrite the import rewrite
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public void setImportRewrite(ImportRewrite rewrite) {
		fImportRewrite= rewrite;
	}

	/**
	 * Creates and sets the import rewrite used for this compilation unit.
	 *
	 * @param astRoot the AST for the current CU
	 * @return the created import rewrite
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public ImportRewrite createImportRewrite(CompilationUnit astRoot) {
		fImportRewrite= StubUtility.createImportRewrite(astRoot, true);
		return fImportRewrite;
	}


	@Override
	public void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
		super.addEdits(document, editRoot);
		ASTRewrite rewrite= getRewrite();
		if (rewrite != null) {
			try {
				TextEdit edit= rewrite.rewriteAST();
				editRoot.addChild(edit);
			} catch (IllegalArgumentException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
		if (fImportRewrite != null) {
			editRoot.addChild(fImportRewrite.rewriteImports(new NullProgressMonitor()));
		}
	}

	/**
	 * Returns the rewrite that has been passed in the constructor. Implementors can override this
	 * method to create the rewrite lazily. This method will only be called once.
	 *
	 * @return the rewrite to be used
	 * @throws CoreException when the rewrite could not be created
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		if (fRewrite == null) {
			IStatus status= JavaUIStatus.createError(IStatus.ERROR, "Rewrite not initialized", null); //$NON-NLS-1$
			throw new CoreException(status);
		}
		return fRewrite;
	}
}