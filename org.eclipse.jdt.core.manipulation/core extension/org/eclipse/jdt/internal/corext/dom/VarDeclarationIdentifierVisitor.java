/*******************************************************************************
 * Copyright (c) 2020 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Visitor collecting all definitions of any variable.
 */
public class VarDeclarationIdentifierVisitor extends ASTVisitor {
	private final Set<SimpleName> variableNames= new HashSet<>();
	private final ASTNode startNode;
	private final boolean includeInnerScopes;

	/**
	 * The constructor.
	 *
	 * @param startNode       the {@link ASTNode} which is the scope of the search
	 * @param includeInnerScopes True if the sub blocks should be analyzed
	 */
	public VarDeclarationIdentifierVisitor(final ASTNode startNode, final boolean includeInnerScopes) {
		this.startNode= startNode;
		this.includeInnerScopes= includeInnerScopes;
	}

	/**
	 * Get the variable names.
	 *
	 * @return the variable names.
	 */
	public Set<SimpleName> getVariableNames() {
		return variableNames;
	}

	@Override
	public boolean visit(final SingleVariableDeclaration node) {
		variableNames.add(node.getName());
		return true;
	}

	@Override
	public boolean visit(final VariableDeclarationFragment node) {
		variableNames.add(node.getName());
		return true;
	}

	@Override
	public boolean visit(final Block node) {
		return startNode == node || includeInnerScopes;
	}
}
