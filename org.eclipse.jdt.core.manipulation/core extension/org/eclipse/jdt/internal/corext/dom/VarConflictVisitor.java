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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * The variable conflict visitor.
 */
public class VarConflictVisitor extends InterruptibleVisitor {
	private final Set<String> localVariableIds;
	private boolean varConflicting;
	private ASTNode startNode;
	private final boolean includeInnerScopes;

	/**
	 * The constructor.
	 *
	 * @param localVariables The variables that may have the same name as others
	 * @param includeInnerScopes True if the sub blocks should be analyzed
	 */
	public VarConflictVisitor(final Set<SimpleName> localVariables, final boolean includeInnerScopes) {
		this.includeInnerScopes= includeInnerScopes;
		this.localVariableIds= new HashSet<>(localVariables.size());

		for (SimpleName localVariable : localVariables) {
			this.localVariableIds.add(localVariable.getIdentifier());
		}
	}

	/**
	 * Returns true if at least one variable is used.
	 *
	 * @return True if at least one variable is used
	 */
	public boolean isVarConflicting() {
		return varConflicting;
	}

	@Override
	public void traverseNodeInterruptibly(final ASTNode aStartNode) {
		this.startNode= aStartNode;
		super.traverseNodeInterruptibly(this.startNode);
	}

	@Override
	public boolean visit(final SimpleName concurrentVariable) {
		if (concurrentVariable.resolveBinding() == null || concurrentVariable.resolveBinding().getKind() == IBinding.VARIABLE) {
			if (localVariableIds.contains(concurrentVariable.getIdentifier())) {
				varConflicting= true;
				return interruptVisit();
			}
		}

		return true;
	}

	@Override
	public boolean visit(final Block node) {
		return startNode == node || includeInnerScopes;
	}
}
