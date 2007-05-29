/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom.fragments;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

class SimpleExpressionFragment extends SimpleFragment implements IExpressionFragment {
	SimpleExpressionFragment(Expression node) {
		super(node);
	}

	public Expression getAssociatedExpression() {
		return (Expression) getAssociatedNode();
	}

	public Expression createCopyTarget(ASTRewrite rewrite) {
		return (Expression) rewrite.createCopyTarget(getAssociatedNode());
	}
}
