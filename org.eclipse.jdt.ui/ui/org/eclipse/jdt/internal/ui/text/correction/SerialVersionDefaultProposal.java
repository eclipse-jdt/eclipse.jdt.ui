/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Proposal for a default serial version id.
 * 
 * @since 3.1
 */
public final class SerialVersionDefaultProposal extends AbstractSerialVersionProposal {

	/** The initializer linked position group id */
	private static final String GROUP_INITIALIZER= "initializer"; //$NON-NLS-1$

	/**
	 * Creates a new serial version default proposal.
	 * 
	 * @param unit
	 *        the compilation unit
	 * @param node
	 *        the originally selected node
	 */
	public SerialVersionDefaultProposal(final ICompilationUnit unit, final ASTNode node) {
		super(CorrectionMessages.getString("SerialVersionSubProcessor.createdefault.description"), unit, node); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractSerialVersionProposal#addInitializer(org.eclipse.jdt.core.dom.VariableDeclarationFragment)
	 */
	protected final void addInitializer(final VariableDeclarationFragment fragment) {
		Assert.isNotNull(fragment);

		final Expression expression= computeDefaultExpression(new NullProgressMonitor());
		if (expression != null)
			fragment.setInitializer(expression);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractSerialVersionProposal#addLinkedPositions(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, org.eclipse.jdt.core.dom.VariableDeclarationFragment)
	 */
	protected final void addLinkedPositions(final ASTRewrite rewrite, final VariableDeclarationFragment fragment) {

		Assert.isNotNull(rewrite);
		Assert.isNotNull(fragment);

		final Expression initializer= fragment.getInitializer();
		if (initializer != null)
			addLinkedPosition(rewrite.track(initializer), true, GROUP_INITIALIZER);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.AbstractSerialVersionProposal#computeDefaultExpression(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected final Expression computeDefaultExpression(final IProgressMonitor monitor) {
		return getAST().newNumberLiteral(DEFAULT_EXPRESSION);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public final String getAdditionalProposalInfo() {
		return CorrectionMessages.getString("SerialVersionDefaultProposal.message.default.info"); //$NON-NLS-1$
	}
}