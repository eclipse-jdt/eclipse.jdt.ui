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

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;

/**
 * Ordered infix expression for commutative operations.
 *
 * @param <F> First operand
 * @param <S> Second operand
 */
public class OrderedInfixExpression<F extends Expression, S extends Expression> {
	private F firstOperand;
	private InfixExpression.Operator operator;
	private S secondOperand;

	/**
	 * Ordered infix expression.
	 *
	 * @param firstOperand first operand
	 * @param operator operator
	 * @param secondOperand second operand
	 */
	public OrderedInfixExpression(final F firstOperand, InfixExpression.Operator operator, final S secondOperand) {
		this.firstOperand= firstOperand;
		this.operator= operator;
		this.secondOperand= secondOperand;
	}

	/**
	 * Get the first operand.
	 *
	 * @return the first operand.
	 */
	public F getFirstOperand() {
		return firstOperand;
	}

	/**
	 * Get the operator.
	 *
	 * @return the operator.
	 */
	public InfixExpression.Operator getOperator() {
		return operator;
	}

	/**
	 * Get the second operand.
	 *
	 * @return the second operand.
	 */
	public S getSecondOperand() {
		return secondOperand;
	}
}
