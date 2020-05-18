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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

/**
 * An interruptible visitor.
 */
public class InterruptibleVisitor extends ASTVisitor {
    /**
     * Traverse the node, silently swallowing {@link AbortSearchException}.
     *
     * @param node The visited node.
     */
    public void traverseNodeInterruptibly(ASTNode node) {
        try {
            node.accept(this);
        } catch (AbortSearchException e) {
            return;
        }
    }

    /**
     * Interrupt the visit of a tree.
     *
     * @return nothing
     */
    public boolean interruptVisit() {
        throw new AbortSearchException();
    }
}
