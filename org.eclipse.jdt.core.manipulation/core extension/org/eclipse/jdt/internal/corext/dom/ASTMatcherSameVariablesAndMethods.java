/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation from AutoRefactor
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.Objects;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * Matches two piece of code only if the variables/methods in use are the same.
 */
public final class ASTMatcherSameVariablesAndMethods extends ASTSemanticMatcher {
	@Override
	public boolean match(final SimpleName node, final Object other) {
		return super.match(node, other) && sameReference(node, (SimpleName) other);
	}

	private boolean sameReference(final SimpleName node1, final SimpleName node2) {
		IBinding declaration1= getDeclaration(node1);
		IBinding declaration2= getDeclaration(node2);
		return declaration1 != null && Objects.equals(declaration1, declaration2);
	}

	private IBinding getDeclaration(final SimpleName node) {
		IBinding ast= node.resolveBinding();

		if (ast != null) {
			switch (ast.getKind()) {
				case IBinding.VARIABLE:
					return ((IVariableBinding) ast).getVariableDeclaration();

				case IBinding.METHOD:
					return ((IMethodBinding) ast).getMethodDeclaration();
				default:
					break;
			}
		}

		return ast;
	}
}
