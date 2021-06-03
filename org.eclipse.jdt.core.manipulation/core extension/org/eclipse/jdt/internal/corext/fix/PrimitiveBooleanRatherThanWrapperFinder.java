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
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;

public final class PrimitiveBooleanRatherThanWrapperFinder extends AbstractPrimitiveRatherThanWrapperFinder {
	public PrimitiveBooleanRatherThanWrapperFinder(final List<CompilationUnitRewriteOperation> ops) {
		fResult= ops;
	}

	@Override
	public String getPrimitiveTypeName() {
		return boolean.class.getSimpleName();
	}

	@Override
	public Class<? extends Expression> getLiteralClass() {
		return BooleanLiteral.class;
	}

	@Override
	public List<PrefixExpression.Operator> getPrefixInSafeOperators() {
		return Arrays.<PrefixExpression.Operator>asList(PrefixExpression.Operator.NOT);
	}

	@Override
	public List<InfixExpression.Operator> getInfixInSafeOperators() {
		return Arrays.<InfixExpression.Operator>asList(InfixExpression.Operator.AND, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.EQUALS, InfixExpression.Operator.GREATER,
				InfixExpression.Operator.GREATER_EQUALS, InfixExpression.Operator.LESS, InfixExpression.Operator.LESS_EQUALS, InfixExpression.Operator.NOT_EQUALS, InfixExpression.Operator.OR, InfixExpression.Operator.XOR);
	}

	@Override
	public List<PrefixExpression.Operator> getPrefixOutSafeOperators() {
		return Arrays.<PrefixExpression.Operator>asList(PrefixExpression.Operator.NOT);
	}

	@Override
	public List<InfixExpression.Operator> getInfixOutSafeOperators() {
		return Arrays.<InfixExpression.Operator>asList(InfixExpression.Operator.AND, InfixExpression.Operator.OR, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.XOR);
	}

	@Override
	public List<Assignment.Operator> getAssignmentOutSafeOperators() {
		return Arrays.<Assignment.Operator>asList(Assignment.Operator.BIT_AND_ASSIGN, Assignment.Operator.BIT_OR_ASSIGN, Assignment.Operator.BIT_XOR_ASSIGN);
	}

	@Override
	public String[] getSafeInConstants() {
		return new String[] { "TRUE", "FALSE" }; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public boolean isSpecificPrimitiveAllowed(final ASTNode node) {
		ASTNode parentNode= node.getParent();

		switch (parentNode.getNodeType()) {
		case ASTNode.IF_STATEMENT:
		case ASTNode.WHILE_STATEMENT:
		case ASTNode.DO_STATEMENT:
			return true;

		default:
			return false;
		}
	}
}
