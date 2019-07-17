/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - code modified from SerialVersionDefaultOperation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;


/**
 * Proposal for a default serial version id.
 *
 * @since 3.1
 */
public final class SerialVersionDefaultOperationCore extends AbstractSerialVersionOperationCore {

	/** The initializer linked position group id */
	private static final String GROUP_INITIALIZER= "initializer"; //$NON-NLS-1$

	/**
	 * Creates a new serial version default proposal.
	 *
	 * @param unit
	 *            the compilation unit
	 * @param nodes
	 *            the originally selected nodes
	 */
	public SerialVersionDefaultOperationCore(ICompilationUnit unit, ASTNode[] nodes) {
		super(unit, nodes);
	}


	@Override
	protected boolean addInitializer(final VariableDeclarationFragment fragment, final ASTNode declarationNode) {
		Assert.isNotNull(fragment);

		final Expression expression= fragment.getAST().newNumberLiteral(DEFAULT_EXPRESSION);
		if (expression != null)
			fragment.setInitializer(expression);
		return true;
	}

	@Override
	protected void addLinkedPositions(final ASTRewrite rewrite, final VariableDeclarationFragment fragment, final LinkedProposalModelCore positionGroups) {

		Assert.isNotNull(rewrite);
		Assert.isNotNull(fragment);

		final Expression initializer= fragment.getInitializer();
		if (initializer != null) {
			LinkedProposalPositionGroupCore group= positionGroups.createPositionGroup(GROUP_INITIALIZER);
			group.addPosition(rewrite.track(initializer), true);
			positionGroups.addPositionGroup(group);
		}
	}

}
