/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix.helper;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public final class LambdaQueries {
	private LambdaQueries() {

	}

	public static Expression getSingleExpressionFromLambdaBody(Block lambdaBody) {
		if (lambdaBody.statements().size() != 1)
			return null;
		Statement singleStatement= (Statement) lambdaBody.statements().get(0);
		if (singleStatement instanceof ReturnStatement) {
			return ((ReturnStatement) singleStatement).getExpression();
		} else if (singleStatement instanceof ExpressionStatement) {
			Expression expression= ((ExpressionStatement) singleStatement).getExpression();
			if (isValidLambdaExpressionBody(expression)) {
				return expression;
			}
		}
		return null;
	}

	private static boolean isValidLambdaExpressionBody(Expression expression) {
		if (expression instanceof Assignment
				|| expression instanceof ClassInstanceCreation
				|| expression instanceof MethodInvocation
				|| expression instanceof PostfixExpression
				|| expression instanceof SuperMethodInvocation) {
			return true;
		}
		if (expression instanceof PrefixExpression) {
			Operator operator= ((PrefixExpression) expression).getOperator();
			if (operator == Operator.INCREMENT || operator == Operator.DECREMENT) {
				return true;
			}
		}
		return false;
	}
}
