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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public abstract class AbstractPrimitiveRatherThanWrapperFinder extends ASTVisitor {
	protected List<CompilationUnitRewriteOperation> fResult;

	/**
	 * Get the primitive type name.
	 *
	 * @return the primitive type name.
	 */
	public abstract String getPrimitiveTypeName();

	/**
	 * Get the literal class.
	 *
	 * @return the literal class.
	 */
	public abstract Class<? extends Expression> getLiteralClass();

	/**
	 * Get the wrapper fully qualified name.
	 *
	 * @return the wrapper fully qualified name.
	 */
	public String getWrapperFullyQualifiedName() {
		return Bindings.getBoxedTypeName(getPrimitiveTypeName());
	}

	/**
	 * Get the prefix in safe operators.
	 *
	 * @return the prefix in safe operators.
	 */
	public List<PrefixExpression.Operator> getPrefixInSafeOperators() {
		return new ArrayList<>(0);
	}

	/**
	 * Get the Infix In Safe Operators.
	 *
	 * @return the Infix In Safe Operators.
	 */
	public List<InfixExpression.Operator> getInfixInSafeOperators() {
		return Collections.emptyList();
	}

	/**
	 * Get the postfix in safe operators.
	 *
	 * @return the postfix in safe operators.
	 */
	public List<PostfixExpression.Operator> getPostfixInSafeOperators() {
		return Collections.emptyList();
	}

	/**
	 * Get the prefix out safe operators.
	 *
	 * @return the prefix out safe operators.
	 */
	public List<PrefixExpression.Operator> getPrefixOutSafeOperators() {
		return Collections.emptyList();
	}

	/**
	 * Get the infix out safe operators.
	 *
	 * @return the infix out safe operators.
	 */
	public List<InfixExpression.Operator> getInfixOutSafeOperators() {
		return Collections.emptyList();
	}

	/**
	 * Get the postfix out safe operators.
	 *
	 * @return the postfix out safe operators.
	 */
	public List<PostfixExpression.Operator> getPostfixOutSafeOperators() {
		return Collections.emptyList();
	}

	/**
	 * Get the assignment out safe operators.
	 *
	 * @return the assignment out safe operators.
	 */
	public List<Assignment.Operator> getAssignmentOutSafeOperators() {
		return Collections.emptyList();
	}

	/**
	 * Get the safe in constants.
	 *
	 * @return the safe in constants.
	 */
	public String[] getSafeInConstants() {
		return new String[0];
	}

	/**
	 * True if the specific primitive is allowed.
	 *
	 * @param node The node
	 *
	 * @return True if the specific primitive is allowed.
	 */
	public boolean isSpecificPrimitiveAllowed(final ASTNode node) {
		return false;
	}

	@Override
	public boolean visit(final VariableDeclarationStatement visited) {
		VariableDeclarationFragment fragment= ASTNodes.getUniqueFragment(visited);

		if (fragment != null
				&& fragment.getInitializer() != null
				&& (isNotNull(fragment.getInitializer()) || canReturnPrimitiveInstead(fragment.getInitializer()))
				&& (fragment.resolveBinding() != null && ASTNodes.hasType(fragment.resolveBinding().getType(), getWrapperFullyQualifiedName())
						|| visited.getType() != null && visited.getType().resolveBinding() != null && ASTNodes.hasType(visited.getType().resolveBinding(), getWrapperFullyQualifiedName()))) {
			VarOccurrenceVisitor varOccurrenceVisitor= new VarOccurrenceVisitor(fragment);
			Block parentBlock= ASTNodes.getTypedAncestor(fragment, Block.class);

			if (parentBlock != null) {
				varOccurrenceVisitor.traverseNodeInterruptibly(parentBlock);
				int boxingCount= varOccurrenceVisitor.getAutoBoxingCount();

				if (ASTNodes.hasType(fragment.getInitializer(), getWrapperFullyQualifiedName()) && !canReturnPrimitiveInstead(fragment.getInitializer())) {
					boxingCount++;
				}

				if (varOccurrenceVisitor.isPrimitiveAllowed() && boxingCount < 2) {
					fResult.add(new PrimitiveRatherThanWrapperOperation(
							visited,
							getPrimitiveTypeName(),
							getWrapperFullyQualifiedName(),
							fragment.getInitializer(),
							varOccurrenceVisitor.getToStringMethods(),
							varOccurrenceVisitor.getCompareToMethods(),
							varOccurrenceVisitor.getPrimitiveValueMethods(),
							getParsingMethodName(getWrapperFullyQualifiedName(), (CompilationUnit) visited.getRoot())));
					return false;
				}
			}
		}

		return true;
	}

	private boolean isNotNull(final Expression expression) {
		if (expression instanceof ParenthesizedExpression) {
			ParenthesizedExpression parenthesizedExpression= (ParenthesizedExpression) expression;
			return isNotNull(parenthesizedExpression.getExpression());
		}

		if (expression instanceof ConditionalExpression) {
			ConditionalExpression prefixExpression= (ConditionalExpression) expression;
			return isNotNull(prefixExpression.getThenExpression()) && isNotNull(prefixExpression.getElseExpression());
		}

		if (getLiteralClass().equals(expression.getClass())) {
			return true;
		}

		if (expression instanceof QualifiedName) {
			QualifiedName qualifiedName= (QualifiedName) expression;
			return ASTNodes.hasType(qualifiedName.getQualifier(), getWrapperFullyQualifiedName())
					&& (ASTNodes.isField(qualifiedName, getWrapperFullyQualifiedName(), getSafeInConstants())
							|| ASTNodes.isField(qualifiedName, getPrimitiveTypeName(), getSafeInConstants()));
		}

		if (expression instanceof InfixExpression) {
			InfixExpression infixExpression= (InfixExpression) expression;
			return getInfixInSafeOperators().contains(infixExpression.getOperator());
		}

		if (expression instanceof PrefixExpression) {
			PrefixExpression prefixExpression= (PrefixExpression) expression;
			return getPrefixInSafeOperators().contains(prefixExpression.getOperator());
		}

		if (expression instanceof PostfixExpression) {
			PostfixExpression postfixExpression= (PostfixExpression) expression;
			return getPostfixInSafeOperators().contains(postfixExpression.getOperator());
		}

		if (expression instanceof CastExpression) {
			CastExpression castExpression= (CastExpression) expression;
			return ASTNodes.hasType(castExpression.getType().resolveBinding(), getPrimitiveTypeName())
					|| ASTNodes.hasType(castExpression.getType().resolveBinding(), getWrapperFullyQualifiedName())
							&& isNotNull(castExpression.getExpression());
		}

		if (expression instanceof ClassInstanceCreation) {
			ClassInstanceCreation classInstanceCreation= (ClassInstanceCreation) expression;
			List<Expression> classInstanceCreationArguments= classInstanceCreation.arguments();

			if (classInstanceCreationArguments.size() == 1) {
				Expression arg0= classInstanceCreationArguments.get(0);

				return ASTNodes.hasType(arg0, String.class.getCanonicalName()) || ASTNodes.hasType(arg0, getPrimitiveTypeName());
			}
		}

		return false;
	}

	private boolean canReturnPrimitiveInstead(final Expression expression) {
		MethodInvocation methodInvocation= ASTNodes.as(expression, MethodInvocation.class);

		if (methodInvocation != null) {
			return ASTNodes.usesGivenSignature(methodInvocation, getWrapperFullyQualifiedName(), "valueOf", getPrimitiveTypeName()) //$NON-NLS-1$
					|| getParsingMethodName(getWrapperFullyQualifiedName(), (CompilationUnit) expression.getRoot()) != null
					&& (
							ASTNodes.usesGivenSignature(methodInvocation, getWrapperFullyQualifiedName(), "valueOf", String.class.getCanonicalName()) //$NON-NLS-1$
							|| ASTNodes.usesGivenSignature(methodInvocation, getWrapperFullyQualifiedName(), "valueOf", String.class.getCanonicalName(), int.class.getSimpleName()) //$NON-NLS-1$
							);
		}

		ClassInstanceCreation classInstanceCreation= ASTNodes.as(expression, ClassInstanceCreation.class);
		if (classInstanceCreation != null) {
			List<Expression> classInstanceCreationArguments= classInstanceCreation.arguments();

			if (classInstanceCreationArguments.size() == 1) {
				Expression arg0= classInstanceCreationArguments.get(0);

				return ASTNodes.hasType(arg0, String.class.getCanonicalName()) || ASTNodes.hasType(arg0, getPrimitiveTypeName());
			}
		}

		return false;
	}

	private String getParsingMethodName(final String wrapperFullyQualifiedName, final CompilationUnit compilationUnit) {
		if (Boolean.class.getCanonicalName().equals(wrapperFullyQualifiedName) && JavaModelUtil.is50OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			return "parseBoolean"; //$NON-NLS-1$
		}

		if (Integer.class.getCanonicalName().equals(wrapperFullyQualifiedName)) {
			return "parseInt"; //$NON-NLS-1$
		}

		if (Long.class.getCanonicalName().equals(wrapperFullyQualifiedName)) {
			return "parseLong"; //$NON-NLS-1$
		}

		if (Double.class.getCanonicalName().equals(wrapperFullyQualifiedName) && JavaModelUtil.is1d2OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			return "parseDouble"; //$NON-NLS-1$
		}

		if (Float.class.getCanonicalName().equals(wrapperFullyQualifiedName) && JavaModelUtil.is1d2OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			return "parseFloat"; //$NON-NLS-1$
		}

		if (Short.class.getCanonicalName().equals(wrapperFullyQualifiedName)) {
			return "parseShort"; //$NON-NLS-1$
		}

		if (Byte.class.getCanonicalName().equals(wrapperFullyQualifiedName)) {
			return "parseByte"; //$NON-NLS-1$
		}

		return null;
	}

	private class VarOccurrenceVisitor extends InterruptibleVisitor {
		private final VariableDeclarationFragment varDecl;
		private final List<MethodInvocation> toStringMethods = new ArrayList<>();
		private final List<MethodInvocation> compareToMethods = new ArrayList<>();
		private final List<MethodInvocation> primitiveValueMethods = new ArrayList<>();
		private boolean isPrimitiveAllowed= true;
		private boolean isVarReturned;
		private int autoBoxingCount;

		public VarOccurrenceVisitor(final VariableDeclarationFragment var) {
			varDecl= var;
		}

		public List<MethodInvocation> getToStringMethods() {
			return toStringMethods;
		}

		public List<MethodInvocation> getCompareToMethods() {
			return compareToMethods;
		}

		public List<MethodInvocation> getPrimitiveValueMethods() {
			return primitiveValueMethods;
		}

		public boolean isPrimitiveAllowed() {
			return isPrimitiveAllowed;
		}

		public int getAutoBoxingCount() {
			return autoBoxingCount;
		}

		@Override
		public boolean visit(final SimpleName aVar) {
			if (isPrimitiveAllowed && ASTNodes.isSameVariable(aVar, varDecl.getName())
					&& !aVar.getParent().equals(varDecl)) {
				isPrimitiveAllowed= isPrimitiveAllowed(aVar);

				if (!isPrimitiveAllowed) {
					return interruptVisit();
				}
			}

			return true;
		}

		private boolean isPrimitiveAllowed(final ASTNode node) {
			ASTNode parentNode= node.getParent();

			switch (parentNode.getNodeType()) {
			case ASTNode.PARENTHESIZED_EXPRESSION:
				return isPrimitiveAllowed(parentNode);

			case ASTNode.CAST_EXPRESSION:
				CastExpression castExpression= (CastExpression) parentNode;
				return ASTNodes.hasType(castExpression.getType().resolveBinding(), getPrimitiveTypeName());

			case ASTNode.ASSIGNMENT:
				Assignment assignment= (Assignment) parentNode;

				if (getAssignmentOutSafeOperators().contains(assignment.getOperator())) {
					return true;
				}

				if (assignment.getLeftHandSide().equals(node)) {
					return isNotNull(assignment.getRightHandSide());
				}

				if (assignment.getRightHandSide().equals(node)) {
					if (assignment.getLeftHandSide() instanceof Name) {
						return isOfType(((Name) assignment.getLeftHandSide()).resolveTypeBinding());
					}

					if (assignment.getLeftHandSide() instanceof FieldAccess) {
						return isOfType(((FieldAccess) assignment.getLeftHandSide()).resolveTypeBinding());
					}

					if (assignment.getLeftHandSide() instanceof SuperFieldAccess) {
						return isOfType(((SuperFieldAccess) assignment.getLeftHandSide()).resolveTypeBinding());
					}
				}

				return false;

			case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				VariableDeclarationFragment fragment= (VariableDeclarationFragment) parentNode;
				return node.getLocationInParent() == VariableDeclarationFragment.INITIALIZER_PROPERTY && isOfType(fragment.getName().resolveTypeBinding());

			case ASTNode.RETURN_STATEMENT:
				if (node.getLocationInParent() == ReturnStatement.EXPRESSION_PROPERTY) {
					MethodDeclaration method= ASTNodes.getTypedAncestor(parentNode, MethodDeclaration.class);

					if (method != null && method.getReturnType2() != null) {
						if (ASTNodes.hasType(method.getReturnType2().resolveBinding(), getPrimitiveTypeName())) {
							return true;
						}

						if (ASTNodes.hasType(method.getReturnType2().resolveBinding(), getWrapperFullyQualifiedName())) {
							if (!isVarReturned) {
								isVarReturned= true;
								autoBoxingCount++;
							}

							return true;
						}
					}
				}

				return false;

			case ASTNode.CONDITIONAL_EXPRESSION:
				return node.getLocationInParent() == ConditionalExpression.EXPRESSION_PROPERTY;

			case ASTNode.PREFIX_EXPRESSION:
				return getPrefixOutSafeOperators().contains(((PrefixExpression) parentNode).getOperator());

			case ASTNode.INFIX_EXPRESSION:
				InfixExpression infixExpression= (InfixExpression) parentNode;
				Operator operator= infixExpression.getOperator();
				if (InfixExpression.Operator.EQUALS.equals(operator) || InfixExpression.Operator.NOT_EQUALS.equals(operator)) {
					Expression leftOperand= infixExpression.getLeftOperand();
					Expression rightOperand= infixExpression.getRightOperand();
					return isNotNull(node.equals(leftOperand) ? rightOperand : leftOperand);
				}
				return getInfixOutSafeOperators().contains(((InfixExpression) parentNode).getOperator());

			case ASTNode.POSTFIX_EXPRESSION:
				return getPostfixOutSafeOperators().contains(((PostfixExpression) parentNode).getOperator());

			case ASTNode.METHOD_INVOCATION:
				MethodInvocation methodInvocation= (MethodInvocation) parentNode;

				if (node.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY) {
					if (ASTNodes.usesGivenSignature(methodInvocation, getWrapperFullyQualifiedName(), getPrimitiveTypeName() + "Value")) { //$NON-NLS-1$
						primitiveValueMethods.add(methodInvocation);
						return true;
					}

					if (ASTNodes.usesGivenSignature(methodInvocation, getWrapperFullyQualifiedName(), "toString")) { //$NON-NLS-1$
						toStringMethods.add(methodInvocation);
						return true;
					}

					if (ASTNodes.usesGivenSignature(methodInvocation, getWrapperFullyQualifiedName(), "compareTo", getWrapperFullyQualifiedName())) { //$NON-NLS-1$
						if (ASTNodes.hasType((Expression) methodInvocation.arguments().get(0), getWrapperFullyQualifiedName())) {
							autoBoxingCount++;
						}

						compareToMethods.add(methodInvocation);
						return true;
					}
				}

				break;

			default:
			}

			return isSpecificPrimitiveAllowed(node);
		}

		private boolean isOfType(final ITypeBinding resolveTypeBinding) {
			if (ASTNodes.hasType(resolveTypeBinding, getPrimitiveTypeName())) {
				return true;
			}

			if (ASTNodes.hasType(resolveTypeBinding, getWrapperFullyQualifiedName())) {
				autoBoxingCount++;
				return true;
			}

			return false;
		}
	}
}
