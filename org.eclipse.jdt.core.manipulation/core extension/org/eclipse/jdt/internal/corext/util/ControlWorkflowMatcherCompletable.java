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

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;

/**
 * Represents an expected workflow.
 */
public interface ControlWorkflowMatcherCompletable extends ControlWorkflowMatcherCreable {
	/**
	 * Add a condition to the existing ones on the current workflow. All the conditions form an AND expression.
	 *
	 * @param expectedCondition The new condition.
	 * @return The matcher
	 */
	ControlWorkflowMatcherCompletable condition(NodeMatcher<Expression> expectedCondition);

	/**
	 * Add a statement that should be found following the workflow conditions.
	 *
	 * @param expectedStatement The statement
	 * @return The matcher
	 */
	ControlWorkflowMatcherCompletable statement(NodeMatcher<Statement> expectedStatement);

	/**
	 * Add a returned value that should be found following the workflow conditions.
	 *
	 * @param expectedExpression The returned value
	 * @return The matcher
	 */
	ControlWorkflowMatcherRunnable returnedValue(NodeMatcher<Expression> expectedExpression);
}
