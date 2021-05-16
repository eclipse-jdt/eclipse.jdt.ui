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
package org.eclipse.jdt.internal.corext.fix;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;

public final class PrimitiveFloatRatherThanWrapperFinder extends AbstractPrimitiveRatherThanWrapperFinder {
	public PrimitiveFloatRatherThanWrapperFinder(final List<CompilationUnitRewriteOperation> ops) {
		fResult= ops;
	}

	@Override
	public String getPrimitiveTypeName() {
		return float.class.getSimpleName();
	}

	@Override
	public Class<? extends Expression> getLiteralClass() {
		return NumberLiteral.class;
	}

	@Override
	public List<PrefixExpression.Operator> getPrefixInSafeOperators() {
		return Arrays.<PrefixExpression.Operator>asList(PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.MINUS, PrefixExpression.Operator.DECREMENT,
				PrefixExpression.Operator.PLUS, PrefixExpression.Operator.COMPLEMENT);
	}

	@Override
	public List<InfixExpression.Operator> getInfixInSafeOperators() {
		return Arrays.<InfixExpression.Operator>asList(InfixExpression.Operator.DIVIDE, InfixExpression.Operator.MINUS,
				InfixExpression.Operator.PLUS, InfixExpression.Operator.REMAINDER, InfixExpression.Operator.TIMES);
	}

	@Override
	public List<PostfixExpression.Operator> getPostfixInSafeOperators() {
		return Arrays.<PostfixExpression.Operator>asList(PostfixExpression.Operator.INCREMENT,
				PostfixExpression.Operator.DECREMENT);
	}

	@Override
	public List<PrefixExpression.Operator> getPrefixOutSafeOperators() {
		return Arrays.<PrefixExpression.Operator>asList(PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.MINUS, PrefixExpression.Operator.DECREMENT,
				PrefixExpression.Operator.PLUS, PrefixExpression.Operator.COMPLEMENT);
	}

	@Override
	public List<InfixExpression.Operator> getInfixOutSafeOperators() {
		return Arrays.<InfixExpression.Operator>asList(InfixExpression.Operator.DIVIDE,
				InfixExpression.Operator.GREATER, InfixExpression.Operator.GREATER_EQUALS,
				InfixExpression.Operator.LESS, InfixExpression.Operator.LESS_EQUALS, InfixExpression.Operator.MINUS,
				InfixExpression.Operator.PLUS, InfixExpression.Operator.REMAINDER, InfixExpression.Operator.TIMES);
	}

	@Override
	public List<PostfixExpression.Operator> getPostfixOutSafeOperators() {
		return Arrays.<PostfixExpression.Operator>asList(PostfixExpression.Operator.INCREMENT,
				PostfixExpression.Operator.DECREMENT);
	}

	@Override
	public List<Assignment.Operator> getAssignmentOutSafeOperators() {
		return Arrays.<Assignment.Operator>asList(Assignment.Operator.PLUS_ASSIGN, Assignment.Operator.MINUS_ASSIGN, Assignment.Operator.TIMES_ASSIGN, Assignment.Operator.DIVIDE_ASSIGN,
				Assignment.Operator.REMAINDER_ASSIGN);
	}

	@Override
	public String[] getSafeInConstants() {
		return new String[] { "MIN_VALUE", "MAX_VALUE", "MIN_NORMAL", "NaN", "NEGATIVE_INFINITY", "POSITIVE_INFINITY" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}
}
