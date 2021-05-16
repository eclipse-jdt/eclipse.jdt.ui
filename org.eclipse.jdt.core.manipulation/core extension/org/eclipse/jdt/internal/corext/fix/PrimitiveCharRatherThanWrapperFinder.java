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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;

public final class PrimitiveCharRatherThanWrapperFinder extends AbstractPrimitiveRatherThanWrapperFinder {
	public PrimitiveCharRatherThanWrapperFinder(final List<CompilationUnitRewriteOperation> ops) {
		fResult= ops;
	}

	@Override
	public String getPrimitiveTypeName() {
		return char.class.getSimpleName();
	}

	@Override
	public Class<? extends Expression> getLiteralClass() {
		return NumberLiteral.class;
	}

	@Override
	public List<PrefixExpression.Operator> getPrefixInSafeOperators() {
		return Arrays.<PrefixExpression.Operator>asList(PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT, PrefixExpression.Operator.COMPLEMENT);
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
		return Arrays.<InfixExpression.Operator>asList(InfixExpression.Operator.AND, InfixExpression.Operator.DIVIDE,
				InfixExpression.Operator.GREATER, InfixExpression.Operator.GREATER_EQUALS,
				InfixExpression.Operator.LEFT_SHIFT, InfixExpression.Operator.LESS,
				InfixExpression.Operator.LESS_EQUALS, InfixExpression.Operator.MINUS, InfixExpression.Operator.OR,
				InfixExpression.Operator.PLUS, InfixExpression.Operator.REMAINDER,
				InfixExpression.Operator.RIGHT_SHIFT_SIGNED, InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED,
				InfixExpression.Operator.TIMES, InfixExpression.Operator.XOR);
	}

	@Override
	public List<PostfixExpression.Operator> getPostfixOutSafeOperators() {
		return Arrays.<PostfixExpression.Operator>asList(PostfixExpression.Operator.INCREMENT,
				PostfixExpression.Operator.DECREMENT);
	}

	@Override
	public List<Assignment.Operator> getAssignmentOutSafeOperators() {
		return Arrays.<Assignment.Operator>asList(Assignment.Operator.PLUS_ASSIGN, Assignment.Operator.MINUS_ASSIGN, Assignment.Operator.TIMES_ASSIGN, Assignment.Operator.DIVIDE_ASSIGN,
				Assignment.Operator.BIT_AND_ASSIGN, Assignment.Operator.BIT_OR_ASSIGN, Assignment.Operator.BIT_XOR_ASSIGN, Assignment.Operator.REMAINDER_ASSIGN, Assignment.Operator.LEFT_SHIFT_ASSIGN,
				Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN, Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN);
	}

	@Override
	public String[] getSafeInConstants() {
		return new String[] {
				"MIN_VALUE", //$NON-NLS-1$
				"MAX_VALUE", //$NON-NLS-1$
				"MAX_HIGH_SURROGATE", //$NON-NLS-1$
				"MAX_LOW_SURROGATE", //$NON-NLS-1$
				"MAX_SURROGATE", //$NON-NLS-1$
				"MIN_HIGH_SURROGATE", //$NON-NLS-1$
				"MIN_LOW_SURROGATE", //$NON-NLS-1$
				"MIN_SURROGATE" //$NON-NLS-1$
				};
	}

	@Override
	public boolean isSpecificPrimitiveAllowed(final ASTNode node) {
		ASTNode parentNode= node.getParent();

		switch (parentNode.getNodeType()) {
		case ASTNode.ARRAY_ACCESS:
		case ASTNode.SWITCH_STATEMENT:
			return true;

		default:
			return false;
		}
	}
}
