/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - copied from SemanticHighlightingReconciler and modified
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;


/**
 * Collects positions from the AST.
 *
 * @since 1.11
 */
public abstract class PositionCollectorCore extends GenericVisitor {

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	@Override
	protected boolean visitNode(ASTNode node) {
		if ((node.getFlags() & ASTNode.MALFORMED) == ASTNode.MALFORMED) {
			retainPositions(node.getStartPosition(), node.getLength());
			return false;
		}
		return true;
	}

	/*
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.BooleanLiteral)
	 */
	@Override
	public boolean visit(BooleanLiteral node) {
		return visitLiteral(node);
	}

	/*
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.CharacterLiteral)
	 */
	@Override
	public boolean visit(CharacterLiteral node) {
		return visitLiteral(node);
	}

	/*
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.NumberLiteral)
	 */
	@Override
	public boolean visit(NumberLiteral node) {
		return visitLiteral(node);
	}

	protected abstract boolean visitLiteral(Expression node);

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.ConstructorInvocation)
	 * @since 3.5
	 */
	@Override
	public abstract boolean visit(ConstructorInvocation node);

	/*
	 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.ConstructorInvocation)
	 * @since 3.5
	 */
	@Override
	public abstract boolean visit(SuperConstructorInvocation node);

	@Override
	public abstract boolean visit(SimpleType node);

	/*
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SimpleName)
	 */
	@Override
	public abstract boolean visit(SimpleName node);

	/**
	 * Retain the positions completely contained in the given range.
	 * @param offset The range offset
	 * @param length The range length
	 */
	protected abstract void retainPositions(int offset, int length);

}
