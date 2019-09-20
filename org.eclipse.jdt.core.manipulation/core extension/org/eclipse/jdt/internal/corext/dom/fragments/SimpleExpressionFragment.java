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

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

class SimpleExpressionFragment extends SimpleFragment implements IExpressionFragment {
	SimpleExpressionFragment(Expression node) {
		super(node);
	}

	@Override
	public Expression getAssociatedExpression() {
		return (Expression) getAssociatedNode();
	}

	@Override
	public Expression createCopyTarget(ASTRewrite rewrite, boolean removeSurroundingParenthesis) {
		Expression node= getAssociatedExpression();
		if (removeSurroundingParenthesis && node instanceof ParenthesizedExpression) {
			node= ((ParenthesizedExpression) node).getExpression();
		}
		return (Expression) rewrite.createCopyTarget(node);
	}
}
