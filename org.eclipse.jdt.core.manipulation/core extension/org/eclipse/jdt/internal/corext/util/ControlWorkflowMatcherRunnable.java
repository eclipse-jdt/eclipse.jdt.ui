/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
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
package org.eclipse.jdt.internal.corext.util;

import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

/**
 * Represents an expected workflow.
 */
public interface ControlWorkflowMatcherRunnable extends ControlWorkflowMatcherCreable {
	/**
	 * Is matching.
	 *
	 * @param actualStatement The actual statement
	 * @return is matching
	 */
	boolean isMatching(Statement actualStatement);

	/**
	 * Is matching.
	 *
	 * @param actualStatements The actual statements
	 * @return is matching
	 */
	boolean isMatching(List<Statement> actualStatements);

	/**
	 * Add a new workflow. A workflow is started by a list of mandatory conditions that are either passive or unordered, a list of statements to encounter and it's ended by an optional returned value.
	 *
	 * @param expectedCondition The expected condition
	 * @return The matcher
	 */
	ControlWorkflowMatcherCompletable addWorkflow(NodeMatcher<Expression> expectedCondition);
}
