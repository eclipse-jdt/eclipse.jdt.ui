/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom.fragments;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 * Represents a fragment (@see IASTFragment) for which the node
 * to which the fragment maps is an Expression.
 */
public interface IExpressionFragment extends IASTFragment {

	/**
	 * Every IASTFragment maps to an ASTNode, although this mapping may
	 * not be straightforward, and more than one fragment may map to the
	 * same node.
	 * An IExpressionFragment maps, specifically, to an Expression.
	 *
	 * @return Expression	The node to which this fragment maps.
	 */
	Expression getAssociatedExpression();

	/**
	 * Creates a copy of this IExpressionFragment.
	 *
	 * @param rewrite an ASTRewrite
	 * @param removeSurroundingParenthesis if set to <code>true</true>, a surrounding ParenthesizedExpression will not be part of
	 * the copied node.
	 * @return a copy of this IExpressionFragment, ready for use in the given
	 *         rewrite
	 * @throws JavaModelException
	 */
	Expression createCopyTarget(ASTRewrite rewrite, boolean removeSurroundingParenthesis) throws JavaModelException;
}
