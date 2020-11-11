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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.core.dom.ASTVisitor;
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
import org.eclipse.jdt.core.dom.SimpleName;
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
		private final IterationType iterationType;
		private final ContainerType containerType;
		private final Expression containerVariable;
		private final Expression iteratorVariable;
		private final Name loopVariable;
		private final boolean isLoopingForward;

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

		if (initializers.isEmpty()
				|| initializers.size() > 2
				|| updaters.size() > 1) {
			return null;
		}

		AtomicBoolean isLoopingForward= new AtomicBoolean();
		Name updater= null;

		if (updaters.size() == 1) {
			updater= getUpdaterOperand(updaters.get(0), isLoopingForward);

			if (updater == null) {
				return null;
			}
		}

		Expression initialVariable;
		Expression initialValue;
		Expression endVariable= null;
		Expression endValue= null;

		List<Expression> initializerVariables= new ArrayList<>();
		List<Expression> initializerValues= new ArrayList<>();

		if (!getInitialization(initializers.get(0), initializerVariables, initializerValues)) {
			return null;
		}

		if (initializers.size() == 2 && !getInitialization(initializers.get(1), initializerVariables, initializerValues)) {
			return null;
		}

		if (initializers.size() > 2) {
			return null;
		}

		if (initializerVariables.size() == 2) {
			if (ASTNodes.isSameVariable(initializerVariables.get(0), updater)) {
				initialVariable= initializerVariables.get(0);
				initialValue= initializerValues.get(0);
				endVariable= initializerVariables.get(1);
				endValue= initializerValues.get(1);
			} else if (ASTNodes.isSameVariable(initializerVariables.get(1), updater)) {
				initialVariable= initializerVariables.get(1);
				initialValue= initializerValues.get(1);
				endVariable= initializerVariables.get(0);
				endValue= initializerValues.get(0);
			} else {
				return null;
			}

			final Expression finalEndVariable= endVariable;

			ASTVisitor variableUseVisitor= new ASTVisitor() {
				@Override
				public boolean visit(final SimpleName aSimpleName) {
					if (ASTNodes.isSameVariable(finalEndVariable, aSimpleName)) {
						throw new AbortSearchException();
					}

					return true;
				}
			};

			try {
				node.getBody().accept(variableUseVisitor);
			} catch (AbortSearchException e) {
				return null;
			}
		} else {
			initialVariable= initializerVariables.get(0);
			initialValue= initializerValues.get(0);
		}

		Expression condition= node.getExpression();

		if (updater == null) {
			if (initializerVariables.size() == 2) {
				return null;
			}

			MethodInvocation conditionMethodInvocation= ASTNodes.as(condition, MethodInvocation.class);
			MethodInvocation initMethodInvocation= ASTNodes.as(initialValue, MethodInvocation.class);

			if (conditionMethodInvocation != null
					&& ASTNodes.isSameVariable(initialVariable, conditionMethodInvocation.getExpression())
					&& ASTNodes.usesGivenSignature(initMethodInvocation, Collection.class.getCanonicalName(), ITERATOR_METHOD)
					&& ASTNodes.usesGivenSignature(conditionMethodInvocation, Iterator.class.getCanonicalName(), HAS_NEXT_METHOD)) {
				return getIteratorOnCollection(initMethodInvocation.getExpression(), conditionMethodInvocation.getExpression());
			}
		} else if (ASTNodes.hasType(initialVariable, int.class.getSimpleName())) {
			InfixExpression startValueMinusOne= ASTNodes.as(initialValue, InfixExpression.class);
			Expression collectionOnSize= null;
			Expression arrayOnLength= null;

			if (startValueMinusOne != null
					&& !startValueMinusOne.hasExtendedOperands()
					&& ASTNodes.hasOperator(startValueMinusOne, InfixExpression.Operator.MINUS)) {
				Long one= ASTNodes.getIntegerLiteral(startValueMinusOne.getRightOperand());

				if (Long.valueOf(1L).equals(one)) {
					collectionOnSize= getCollectionOnSize(startValueMinusOne.getLeftOperand());
					arrayOnLength= getArrayOnLength(startValueMinusOne.getLeftOperand());
				}
			}

			Long initialInteger= ASTNodes.getIntegerLiteral(initialValue);
			ForLoopContent forContent= getIndexOnIterable(condition, endVariable, endValue, initialVariable, collectionOnSize, arrayOnLength, isLoopingForward.get());

			if (forContent != null
					&& ASTNodes.isSameVariable(initialVariable, forContent.loopVariable)
					&& ASTNodes.isSameVariable(initialVariable, updater)
					&& isLoopingForward.get() == Long.valueOf(0L).equals(initialInteger)) {
				return forContent;
			}
		}

		return null;
	}

	private static boolean getInitialization(final Expression initializer, List<Expression> variables, final List<Expression> values) {
		VariableDeclarationExpression variableDeclarationExpression= ASTNodes.as(initializer, VariableDeclarationExpression.class);

		if (variableDeclarationExpression != null) {
			List<VariableDeclarationFragment> fragments= variableDeclarationExpression.fragments();

			if (fragments.size() > 2) {
				return false;
			}

			VariableDeclarationFragment fragment= fragments.get(0);
			variables.add(fragment.getName());
			values.add(fragment.getInitializer());

			if (fragments.size() == 2) {
				fragment= fragments.get(1);
				variables.add(fragment.getName());
				values.add(fragment.getInitializer());
			}

			return true;
		}

		Assignment assignment= ASTNodes.as(initializer, Assignment.class);

		if (assignment != null && ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
			variables.add(assignment.getLeftHandSide());
			values.add(assignment.getRightHandSide());
			return true;
		}

		return false;
	}

	private static ForLoopContent getIteratorOnCollection(final Expression containerVar, final Expression iteratorVariable) {
		if (containerVar instanceof Name || containerVar instanceof FieldAccess || containerVar instanceof SuperFieldAccess) {
			return ForLoopContent.iteratedCollection(containerVar, iteratorVariable);
		}

		return null;
	}

	private static Name getUpdaterOperand(final Expression updater, final AtomicBoolean isLoopingForward) {
		Expression updaterOperand= null;

		if (updater instanceof PostfixExpression) {
			PostfixExpression postfixExpression= (PostfixExpression) updater;

			if (ASTNodes.hasOperator(postfixExpression, PostfixExpression.Operator.INCREMENT)) {
				isLoopingForward.set(true);
				updaterOperand= postfixExpression.getOperand();
			}

			if (ASTNodes.hasOperator(postfixExpression, PostfixExpression.Operator.DECREMENT)) {
				isLoopingForward.set(false);
				updaterOperand= postfixExpression.getOperand();
			}
		} else if (updater instanceof PrefixExpression) {
			PrefixExpression prefixExpression= (PrefixExpression) updater;

			if (ASTNodes.hasOperator(prefixExpression, PrefixExpression.Operator.INCREMENT)) {
				isLoopingForward.set(true);
				updaterOperand= prefixExpression.getOperand();
			}

			if (ASTNodes.hasOperator(prefixExpression, PrefixExpression.Operator.DECREMENT)) {
				isLoopingForward.set(false);
				updaterOperand= prefixExpression.getOperand();
			}
		}

		return ASTNodes.as(updaterOperand, Name.class);
	}

	private static ForLoopContent getIndexOnIterable(final Expression condition,
			final Expression endVariable,
			final Expression endValue,
			final Expression loopVariable,
			final Expression collectionOnSize,
			final Expression arrayOnLength,
			final boolean isLoopingForward) {
		InfixExpression infixExpression= ASTNodes.as(condition, InfixExpression.class);

		if (infixExpression != null && !infixExpression.hasExtendedOperands()) {
			Expression leftOperand= infixExpression.getLeftOperand();
			Expression rightOperand= infixExpression.getRightOperand();

			if (!(loopVariable instanceof Name)) {
				return null;
			}

			if (isLoopingForward) {
				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.LESS, InfixExpression.Operator.NOT_EQUALS) && ASTNodes.isSameLocalVariable(loopVariable, leftOperand)) {
					return buildForLoopContent((Name) loopVariable, rightOperand, endVariable, endValue, isLoopingForward, collectionOnSize, arrayOnLength);
				}

				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.GREATER, InfixExpression.Operator.NOT_EQUALS) && ASTNodes.isSameLocalVariable(loopVariable, rightOperand)) {
					return buildForLoopContent((Name) loopVariable, leftOperand, endVariable, endValue, isLoopingForward, collectionOnSize, arrayOnLength);
				}
			} else if (collectionOnSize != null || arrayOnLength != null) {
				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.GREATER_EQUALS) && ASTNodes.isSameLocalVariable(loopVariable, leftOperand)) {
					return buildForLoopContent((Name) loopVariable, rightOperand, endVariable, endValue, isLoopingForward, collectionOnSize, arrayOnLength);
				}

				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.LESS_EQUALS) && ASTNodes.isSameLocalVariable(loopVariable, rightOperand)) {
					return buildForLoopContent((Name) loopVariable, leftOperand, endVariable, endValue, isLoopingForward, collectionOnSize, arrayOnLength);
				}
			}
		}

		return null;
	}

	private static ForLoopContent buildForLoopContent(
			final Name loopVar,
			final Expression containerVariable,
			final Expression endVariable,
			final Expression endValue,
			final boolean isLoopingForward,
			final Expression collectionOnSize,
			final Expression arrayOnLength) {
		Expression endOfLoop;

		if (endVariable != null) {
			if (!ASTNodes.isSameVariable(containerVariable, endVariable)) {
				return null;
			}

			endOfLoop= endValue;
		} else {
			endOfLoop= containerVariable;
		}

		Long containerZero= ASTNodes.getIntegerLiteral(endOfLoop);
		Expression containerCollectionOnSize= getCollectionOnSize(endOfLoop);
		Expression containerArrayOnLength= getArrayOnLength(endOfLoop);

		if (isLoopingForward) {
			if (containerCollectionOnSize != null) {
				return ForLoopContent.indexedCollection(containerCollectionOnSize, loopVar, true);
			}

			if (containerArrayOnLength != null) {
				return ForLoopContent.indexedArray(containerArrayOnLength, loopVar, true);
			}
		} else if (Long.valueOf(0L).equals(containerZero)) {
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
