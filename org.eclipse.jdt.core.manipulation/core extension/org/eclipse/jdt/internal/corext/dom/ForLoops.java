/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation from AutoRefactor
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/** Helper class for dealing with loops. */
public final class ForLoops {
	private static final String LENGTH= "length"; //$NON-NLS-1$
	private static final String SIZE_METHOD= "size"; //$NON-NLS-1$
	private static final String HAS_NEXT_METHOD= "hasNext"; //$NON-NLS-1$
	private static final String ITERATOR_METHOD= "iterator"; //$NON-NLS-1$

	private ForLoops() {
	}

	/** The element container that the for loop iterates over. */
	public enum ContainerType {
		/** Means the for loop iterates over an array. */
		ARRAY,
		/** Means the for loop iterates over a collection. */
		COLLECTION
	}

	/** The for loop iteration type. */
	public enum IterationType {
		/** The for loop iterates using an integer index. */
		INDEX,
		/** The for loop iterates using an iterator. */
		ITERATOR,
		/**
		 * The for loop iterates via a foreach. Technically this could be desugared by
		 * using an iterator.
		 */
		FOREACH
	}

	/** The content of the for loop. */
	public static final class ForLoopContent {
		private IterationType iterationType;
		private ContainerType containerType;
		private Expression containerVariable;
		private Expression iteratorVariable;
		private Name loopVariable;
		private boolean isLoopingForward;

		private ForLoopContent(final IterationType iterationType, final ContainerType containerType, final Expression containerVariable,
				final Expression iteratorVariable, final Name loopVariable, final boolean isLoopingForward) {
			this.iterationType= iterationType;
			this.containerType= containerType;
			this.containerVariable= containerVariable;
			this.iteratorVariable= iteratorVariable;
			this.loopVariable= loopVariable;
			this.isLoopingForward= isLoopingForward;
		}

		private static ForLoopContent indexedArray(final Expression containerVariable, final Name loopVariable, final boolean isLoopingForward) {
			return new ForLoopContent(IterationType.INDEX, ContainerType.ARRAY, containerVariable, null, loopVariable, isLoopingForward);
		}

		private static ForLoopContent indexedCollection(final Expression containerVariable, final Name loopVariable, final boolean isLoopingForward) {
			return new ForLoopContent(IterationType.INDEX, ContainerType.COLLECTION, containerVariable, null, loopVariable, isLoopingForward);
		}

		private static ForLoopContent iteratedCollection(final Expression containerVariable, final Expression iteratorVariable) {
			return new ForLoopContent(IterationType.ITERATOR, ContainerType.COLLECTION, containerVariable, iteratorVariable, null, true);
		}

		/**
		 * Returns the name of the index variable.
		 *
		 * @return the name of the index variable
		 */
		public Name getLoopVariable() {
			return loopVariable;
		}

		/**
		 * Returns the name of the container variable.
		 *
		 * @return the name of the container variable
		 */
		public Expression getContainerVariable() {
			return containerVariable;
		}

		/**
		 * Returns the name of the iterator variable.
		 *
		 * @return the name of the iterator variable
		 */
		public Expression getIteratorVariable() {
			return iteratorVariable;
		}

		/**
		 * Returns the container type.
		 *
		 * @return the container type
		 */
		public ContainerType getContainerType() {
			return containerType;
		}

		/**
		 * Returns the for loop's iteration type.
		 *
		 * @return the for loop's iteration type
		 */
		public IterationType getIterationType() {
			return iterationType;
		}

		/**
		 * Returns true if the loop iterate from the start to the end of the container.
		 *
		 * @return true if the loop iterate from the start to the end of the container
		 */
		public boolean isLoopingForward() {
			return isLoopingForward;
		}
	}

	/**
	 * Returns the {@link ForLoopContent} if this for loop iterates over a
	 * container.
	 *
	 * @param node the for statement
	 * @return the {@link ForLoopContent} if this for loop iterates over a
	 *         container, null otherwise
	 */
	public static ForLoopContent iterateOverContainer(final ForStatement node) {
		List<Expression> initializers= node.initializers();
		List<Expression> updaters= node.updaters();

		if (initializers.size() != 1 || updaters.size() > 1) {
			return null;
		}

		Expression firstInit= initializers.get(0);
		Expression init= null;
		Expression initialization= null;
		VariableDeclarationExpression variableDeclarationExpression= ASTNodes.as(firstInit, VariableDeclarationExpression.class);
		Assignment assignment= ASTNodes.as(firstInit, Assignment.class);

		if (variableDeclarationExpression != null) {
			List<VariableDeclarationFragment> fragments= variableDeclarationExpression.fragments();

			if (fragments.size() != 1) {
				return null;
			}

			VariableDeclarationFragment fragment= fragments.get(0);
			init= fragment.getName();
			initialization= fragment.getInitializer();
		} else if (assignment != null && ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
			initialization= assignment.getRightHandSide();
			Name name= ASTNodes.as(assignment.getLeftHandSide(), Name.class);
			FieldAccess fieldAccess= ASTNodes.as(assignment.getLeftHandSide(), FieldAccess.class);
			SuperFieldAccess superFieldAccess= ASTNodes.as(assignment.getLeftHandSide(), SuperFieldAccess.class);

			if (name != null) {
				init= name;
			} else if (fieldAccess != null) {
				init= fieldAccess;
			} else if (superFieldAccess != null) {
				init= superFieldAccess;
			} else {
				return null;
			}
		}

		if (init == null || initialization == null) {
			return null;
		}

		Expression condition= node.getExpression();

		if (updaters.isEmpty()) {
			MethodInvocation condMi= ASTNodes.as(condition, MethodInvocation.class);
			MethodInvocation initMi= ASTNodes.as(initialization, MethodInvocation.class);

			if (condMi != null && ASTNodes.isSameVariable(init, condMi.getExpression())
					&& ASTNodes.usesGivenSignature(initMi, Collection.class.getCanonicalName(), ITERATOR_METHOD)
					&& ASTNodes.usesGivenSignature(condMi, Iterator.class.getCanonicalName(), HAS_NEXT_METHOD)) {
				return getIteratorOnCollection(initMi.getExpression(), condMi.getExpression());
			}
		} else if (updaters.size() == 1 && ASTNodes.hasType(firstInit, int.class.getSimpleName())) {
			Expression startValue= initialization;
			InfixExpression startValueMinusOne= ASTNodes.as(startValue, InfixExpression.class);
			Expression collectionOnSize= null;
			Expression arrayOnLength= null;

			if (startValueMinusOne != null && !startValueMinusOne.hasExtendedOperands() && ASTNodes.hasOperator(startValueMinusOne, InfixExpression.Operator.MINUS)) {
				Long one= ASTNodes.getIntegerLiteral(startValueMinusOne.getRightOperand());

				if (Long.valueOf(1).equals(one)) {
					collectionOnSize= getCollectionOnSize(startValueMinusOne.getLeftOperand());
					arrayOnLength= getArrayOnLength(startValueMinusOne.getLeftOperand());
				}
			}

			Long zero= ASTNodes.getIntegerLiteral(startValue);
			ForLoopContent forContent= getIndexOnIterable(condition, init, zero, collectionOnSize, arrayOnLength);
			Name updater= getUpdaterOperand(updaters.get(0), Long.valueOf(0).equals(zero));

			if (forContent != null && ASTNodes.isSameVariable(init, forContent.loopVariable)
					&& ASTNodes.isSameVariable(init, updater)) {
				return forContent;
			}
		}

		return null;
	}

	private static ForLoopContent getIteratorOnCollection(final Expression containerVar, final Expression iteratorVariable) {
		if (containerVar instanceof Name || containerVar instanceof FieldAccess || containerVar instanceof SuperFieldAccess) {
			return ForLoopContent.iteratedCollection(containerVar, iteratorVariable);
		}

		return null;
	}

	private static Name getUpdaterOperand(final Expression updater, final boolean isLoopingForward) {
		Expression updaterOperand= null;

		if (updater instanceof PostfixExpression) {
			PostfixExpression postfixExpression= (PostfixExpression) updater;

			if (isLoopingForward ? ASTNodes.hasOperator(postfixExpression, PostfixExpression.Operator.INCREMENT) : ASTNodes.hasOperator(postfixExpression, PostfixExpression.Operator.DECREMENT)) {
				updaterOperand= postfixExpression.getOperand();
			}
		} else if (updater instanceof PrefixExpression) {
			PrefixExpression prefixExpression= (PrefixExpression) updater;

			if (isLoopingForward ? ASTNodes.hasOperator(prefixExpression, PrefixExpression.Operator.INCREMENT) : ASTNodes.hasOperator(prefixExpression, PrefixExpression.Operator.DECREMENT)) {
				updaterOperand= prefixExpression.getOperand();
			}
		}

		return ASTNodes.as(updaterOperand, Name.class);
	}

	private static ForLoopContent getIndexOnIterable(final Expression condition, final Expression loopVariable, final Long zero, final Expression collectionOnSize, final Expression arrayOnLength) {
		InfixExpression infixExpression= ASTNodes.as(condition, InfixExpression.class);

		if (infixExpression != null && !infixExpression.hasExtendedOperands()) {
			Expression leftOp= infixExpression.getLeftOperand();
			Expression rightOp= infixExpression.getRightOperand();

			if (!(loopVariable instanceof Name)) {
				return null;
			}

			if (Long.valueOf(0).equals(zero)) {
				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.LESS, InfixExpression.Operator.NOT_EQUALS) && ASTNodes.isSameLocalVariable(loopVariable, leftOp)) {
					return buildForLoopContent((Name) loopVariable, rightOp, zero, collectionOnSize, arrayOnLength);
				}
				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.GREATER, InfixExpression.Operator.NOT_EQUALS) && ASTNodes.isSameLocalVariable(loopVariable, rightOp)) {
					return buildForLoopContent((Name) loopVariable, leftOp, zero, collectionOnSize, arrayOnLength);
				}
			} else if (collectionOnSize != null || arrayOnLength != null) {
				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.GREATER_EQUALS) && ASTNodes.isSameLocalVariable(loopVariable, leftOp)) {
					return buildForLoopContent((Name) loopVariable, rightOp, zero, collectionOnSize, arrayOnLength);
				}
				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.LESS_EQUALS) && ASTNodes.isSameLocalVariable(loopVariable, rightOp)) {
					return buildForLoopContent((Name) loopVariable, leftOp, zero, collectionOnSize, arrayOnLength);
				}
			}
		}

		return null;
	}

	private static ForLoopContent buildForLoopContent(final Name loopVar, final Expression containerVar, final Long zero, final Expression collectionOnSize, final Expression arrayOnLength) {
		Long containerZero= ASTNodes.getIntegerLiteral(containerVar);
		Expression containerCollectionOnSize= getCollectionOnSize(containerVar);
		Expression containerArrayOnLength= getArrayOnLength(containerVar);

		if (Long.valueOf(0).equals(zero)) {
			if (containerCollectionOnSize != null) {
				return ForLoopContent.indexedCollection(containerCollectionOnSize, loopVar, true);
			}

			if (containerArrayOnLength != null) {
				return ForLoopContent.indexedArray(containerArrayOnLength, loopVar, true);
			}
		} else if (Long.valueOf(0).equals(containerZero)) {
			if (collectionOnSize != null) {
				return ForLoopContent.indexedCollection(collectionOnSize, loopVar, false);
			}

			if (arrayOnLength != null) {
				return ForLoopContent.indexedArray(arrayOnLength, loopVar, false);
			}
		}

		return null;
	}

	private static Expression getCollectionOnSize(final Expression containerVar) {
		MethodInvocation methodInvocation= ASTNodes.as(containerVar, MethodInvocation.class);

		if (methodInvocation != null) {
			Expression containerVarName= ASTNodes.getUnparenthesedExpression(methodInvocation.getExpression());

			if (containerVarName != null && ASTNodes.usesGivenSignature(methodInvocation, Collection.class.getCanonicalName(), SIZE_METHOD)) {
				return containerVarName;
			}
		}

		return null;
	}

	private static Expression getArrayOnLength(final Expression containerVar) {
		if (containerVar instanceof QualifiedName) {
			QualifiedName containerVarName= (QualifiedName) containerVar;

			if (ASTNodes.isArray(containerVarName.getQualifier()) && LENGTH.equals(containerVarName.getName().getIdentifier())) {
				return containerVarName.getQualifier();
			}
		} else if (containerVar instanceof FieldAccess) {
			FieldAccess containerVarName= (FieldAccess) containerVar;

			if (ASTNodes.isArray(containerVarName.getExpression()) && LENGTH.equals(containerVarName.getName().getIdentifier())) {
				return containerVarName.getExpression();
			}
		}

		return null;
	}
}
