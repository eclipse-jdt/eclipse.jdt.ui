/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.java.correction;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * A proposal for quick fixes and quick assists that works on an AST rewrite. Either a rewrite is
 * directly passed in the constructor or the method {@link #getRewrite()} is overridden to provide
 * the AST rewrite that is evaluated on the document when the proposal is applied.
 *
 * @since 3.8
 */
public class ASTRewriteCorrectionProposal extends CUCorrectionProposal {
	/**
	 * Constructs an AST rewrite correction proposal.
	 * @param delegate The delegate instance
	 * @param image the image that is displayed for this proposal or <code>null</code> if no image
	 *            is desired
	 * @since 3.31
	 */
	public ASTRewriteCorrectionProposal(ASTRewriteCorrectionProposalCore delegate, Image image) {
		super(delegate.getName(), delegate.getCompilationUnit(), delegate.getRelevance(), image, delegate);
	}


	/**
	 * Constructs an AST rewrite correction proposal.
	 * @param name the display name of the proposal
	 * @param cu the compilation unit that is modified
	 * @param rewrite the AST rewrite that is invoked when the proposal is applied or
	 *            <code>null</code> if {@link #getRewrite()} is overridden
	 * @param relevance the relevance of this proposal
	 * @param image the image that is displayed for this proposal or <code>null</code> if no image
	 *            is desired
	 * @param delegate The delegate instance
	 * @since 3.31
	 */
	public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image, ASTRewriteCorrectionProposalCore delegate) {
		super(name, cu, relevance, image, delegate);
	}

	/**
	 * Constructs an AST rewrite correction proposal.
	 *
	 * @param name the display name of the proposal
	 * @param cu the compilation unit that is modified
	 * @param rewrite the AST rewrite that is invoked when the proposal is applied or
	 *            <code>null</code> if {@link #getRewrite()} is overridden
	 * @param relevance the relevance of this proposal
	 * @param image the image that is displayed for this proposal or <code>null</code> if no image
	 *            is desired
	 */
	public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, relevance, image, new ASTRewriteCorrectionProposalCore(name, cu, rewrite, relevance));
	}

	/**
	 * Constructs an AST rewrite correction proposal. Uses the default image for this proposal.
	 *
	 * @param name the display name of the proposal
	 * @param cu the compilation unit that is modified
	 * @param rewrite the AST rewrite that is invoked when the proposal is applied or
	 *            <code>null</code> if {@link #getRewrite()} is overridden
	 * @param relevance The relevance of this proposal
	 */
	public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		this(name, cu, rewrite, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}

	public ImportRewrite createImportRewrite(CompilationUnit astRoot) {
		return ((ASTRewriteCorrectionProposalCore)getDelegate()).createImportRewrite(astRoot);
	}

	protected ASTRewrite getRewrite() throws CoreException {
		return ((ASTRewriteCorrectionProposalCore)getDelegate()).getRewrite();
	}

	/**
	 * Returns the import rewrite used for this compilation unit.
	 *
	 * @return the import rewrite or <code>null</code> if no import rewrite has been set
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public ImportRewrite getImportRewrite() {
		return ((ASTRewriteCorrectionProposalCore)getDelegate()).getImportRewrite();
	}

	/**
	 * Sets the import rewrite used for this compilation unit.
	 *
	 * @param rewrite the import rewrite
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public void setImportRewrite(ImportRewrite rewrite) {
		((ASTRewriteCorrectionProposalCore)getDelegate()).setImportRewrite(rewrite);
	}
}
