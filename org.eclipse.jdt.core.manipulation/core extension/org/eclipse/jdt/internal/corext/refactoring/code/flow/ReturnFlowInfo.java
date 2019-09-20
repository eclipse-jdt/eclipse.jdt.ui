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
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ReturnStatement;

class ReturnFlowInfo extends FlowInfo {

	public ReturnFlowInfo(ReturnStatement node) {
		super(getReturnFlag(node));
	}

	public void merge(FlowInfo info, FlowContext context) {
		if (info == null)
			return;

		assignAccessMode(info);
	}

	private static int getReturnFlag(ReturnStatement node) {
		Expression expression= node.getExpression();
		if (expression == null || expression.resolveTypeBinding() == node.getAST().resolveWellKnownType("void")) //$NON-NLS-1$
			return VOID_RETURN;
		return VALUE_RETURN;
	}
}


