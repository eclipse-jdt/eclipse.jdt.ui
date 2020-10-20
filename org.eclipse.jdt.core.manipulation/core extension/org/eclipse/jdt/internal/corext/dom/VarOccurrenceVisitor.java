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

import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * The variable occurrence visitor.
 */
public class VarOccurrenceVisitor extends InterruptibleVisitor {
	private final Set<SimpleName> localVarIds;
	private boolean varUsed;
	private ASTNode startNode;
	private final boolean includeInnerScopes;

	/**
	 * Returns true if at least one variable is used.
	 *
	 * @return True if at least one variable is used
	 */
	public boolean isVarUsed() {
		return varUsed;
	}

	/**
	 * The constructor.
	 *
	 * @param localVarIds The ids of the variable to search
	 * @param includeInnerScopes True if the sub blocks should be analyzed
	 */
	public VarOccurrenceVisitor(final Set<SimpleName> localVarIds, final boolean includeInnerScopes) {
		this.localVarIds= localVarIds;
		this.includeInnerScopes= includeInnerScopes;
	}

	@Override
	public void traverseNodeInterruptibly(final ASTNode aStartNode) {
		this.startNode= aStartNode;
		super.traverseNodeInterruptibly(this.startNode);
	}

	@Override
	public boolean visit(final SimpleName aVariable) {
		for (SimpleName localVarId : localVarIds) {
			if (localVarId.getIdentifier() != null && localVarId.getIdentifier().equals(aVariable.getIdentifier())) {
				varUsed= true;
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
