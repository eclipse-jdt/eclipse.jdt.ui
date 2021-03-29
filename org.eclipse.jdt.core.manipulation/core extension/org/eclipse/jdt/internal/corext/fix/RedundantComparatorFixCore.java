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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class RedundantComparatorFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class RedundantComparatorFinder extends ASTVisitor {
		private List<RedundantComparatorFixOperation> fResult;

		public RedundantComparatorFinder(List<RedundantComparatorFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final MethodInvocation visited) {
			IMethodBinding methodBinding= visited.resolveMethodBinding();

			if (methodBinding != null) {
				if (methodBinding.getParameterTypes().length == 2
						&& ASTNodes.hasType(methodBinding.getDeclaringClass(), Collections.class.getCanonicalName())
						&& ("sort".equals(visited.getName().getIdentifier()) && ASTNodes.hasType(methodBinding.getParameterTypes()[0], List.class.getCanonicalName()) //$NON-NLS-1$
								|| Arrays.asList("max", "min").contains(visited.getName().getIdentifier()) && ASTNodes.hasType(methodBinding.getParameterTypes()[0], Collection.class.getCanonicalName())) //$NON-NLS-1$//$NON-NLS-2$
						&& ASTNodes.hasType(methodBinding.getParameterTypes()[1], Comparator.class.getCanonicalName())) {
					List<Expression> args= visited.arguments();
					return maybeRefactorCode(null, args.get(0), args.get(1));
				}

				if (visited.getExpression() != null
						&& methodBinding.getParameterTypes().length == 1
						&& "sort".equals(visited.getName().getIdentifier()) //$NON-NLS-1$
						&& ASTNodes.hasType(methodBinding.getDeclaringClass(), List.class.getCanonicalName())
						&& ASTNodes.hasType(methodBinding.getParameterTypes()[0], Comparator.class.getCanonicalName())) {
					List<Expression> args= visited.arguments();
					return maybeRefactorCode(visited, visited.getExpression(), args.get(0));
				}
			}

			return true;
		}

		private boolean maybeRefactorCode(final MethodInvocation visitedIfRefactoringNeeded, final Expression list,
				final Expression comparator) {
			if (list.resolveTypeBinding() != null) {
				ITypeBinding[] typeArguments= list.resolveTypeBinding().getTypeArguments();

				if (typeArguments != null
						&& typeArguments.length == 1
						&& isComparable(typeArguments[0])) {
					return maybeRefactorTypedCode(visitedIfRefactoringNeeded, list, comparator, comparator, typeArguments,
							true);
				}
			}

			return true;
		}

		private boolean maybeRefactorTypedCode(
				final MethodInvocation visitedIfRefactoringNeeded,
				final Expression list,
				final Expression comparatorToRemove,
				final Expression comparatorToAnalyze,
				final ITypeBinding[] typeArguments,
				final boolean isForward) {
			NullLiteral nullLiteral= ASTNodes.as(comparatorToAnalyze, NullLiteral.class);

			if (nullLiteral != null) {
				return maybeRemoveComparator(visitedIfRefactoringNeeded, list, comparatorToRemove, isForward);
			}

			ClassInstanceCreation classInstanceCreation= ASTNodes.as(comparatorToAnalyze, ClassInstanceCreation.class);

			if (classInstanceCreation != null
					&& isClassToRemove(classInstanceCreation, isForward)) {
				return maybeRemoveComparator(visitedIfRefactoringNeeded, list, comparatorToRemove, true);
			}

			MethodReference methodReference= ASTNodes.as(comparatorToAnalyze, MethodReference.class);
			LambdaExpression lambdaExpression= ASTNodes.as(comparatorToAnalyze, LambdaExpression.class);
			MethodInvocation methodInvocation= ASTNodes.as(comparatorToAnalyze, MethodInvocation.class);

			if (methodReference != null) {
				String elementClass= typeArguments[0].isWildcardType() ? Comparable.class.getCanonicalName()
						: typeArguments[0].getQualifiedName();

				if (ASTNodes.usesGivenSignature(methodReference.resolveMethodBinding(), elementClass, "compareTo", //$NON-NLS-1$
						Object.class.getCanonicalName())) {
					return maybeRemoveComparator(visitedIfRefactoringNeeded, list, comparatorToRemove, isForward);
				}
			} else if (methodInvocation != null) {
				if (ASTNodes.usesGivenSignature(methodInvocation, Comparator.class.getCanonicalName(), "reversed") //$NON-NLS-1$
						&& methodInvocation.getExpression() != null) {
					return maybeRefactorTypedCode(visitedIfRefactoringNeeded, list, comparatorToRemove,
							methodInvocation.getExpression(), typeArguments, !isForward);
				}

				if (ASTNodes.usesGivenSignature(methodInvocation, Comparator.class.getCanonicalName(), "naturalOrder")) { //$NON-NLS-1$
					return maybeRemoveComparator(visitedIfRefactoringNeeded, list, comparatorToRemove, isForward);
				}

				if ("comparing".equals(methodInvocation.getName().getIdentifier()) //$NON-NLS-1$
						&& methodInvocation.resolveMethodBinding() != null
						&& methodInvocation.resolveMethodBinding().getParameterTypes().length == 1
						&& ASTNodes.hasType(methodInvocation.resolveMethodBinding().getDeclaringClass(),
								Comparator.class.getCanonicalName())
						&& ASTNodes.hasType(methodInvocation.resolveMethodBinding().getParameterTypes()[0],
								Function.class.getCanonicalName())) {
					List<Expression> comparingMethodArgs= methodInvocation.arguments();
					Expression criteria= comparingMethodArgs.get(0);
					LambdaExpression comparingMethodLambdaExpression= ASTNodes.as(criteria, LambdaExpression.class);
					MethodInvocation identityMethod= ASTNodes.as(criteria, MethodInvocation.class);

					if (comparingMethodLambdaExpression != null) {
						if (comparingMethodLambdaExpression.parameters().size() == 1) {
							List<VariableDeclaration> parameters= comparingMethodLambdaExpression.parameters();
							SimpleName variable= parameters.get(0).getName();

							Expression bodyExpression= null;

							if (comparingMethodLambdaExpression.getBody() instanceof Block) {
								ReturnStatement returnStatement= ASTNodes
										.as((Block) comparingMethodLambdaExpression.getBody(), ReturnStatement.class);

								if (returnStatement == null) {
									return true;
								}

								bodyExpression= returnStatement.getExpression();
							} else if (comparingMethodLambdaExpression.getBody() instanceof Expression) {
								bodyExpression= (Expression) comparingMethodLambdaExpression.getBody();
							} else {
								return true;
							}

							if (ASTNodes.areSameVariables(variable, bodyExpression)) {
								return maybeRemoveComparator(visitedIfRefactoringNeeded, list, comparatorToRemove,
										isForward);
							}
						}
					} else if (identityMethod != null
							&& "identity".equals(identityMethod.getName().getIdentifier()) //$NON-NLS-1$
							&& identityMethod.resolveMethodBinding() != null
							&& identityMethod.resolveMethodBinding().getParameterTypes().length == 0
							&& ASTNodes.hasType(identityMethod.resolveMethodBinding().getDeclaringClass(),
									Function.class.getCanonicalName())) {
						return maybeRemoveComparator(visitedIfRefactoringNeeded, list, comparatorToRemove, isForward);
					}
				}
			} else if (lambdaExpression != null) {
				if (lambdaExpression.parameters().size() == 2) {
					List<ASTNode> parameters= lambdaExpression.parameters();
					ASTNode parameter1= parameters.get(0);
					ASTNode parameter2= parameters.get(1);

					SimpleName variable1;
					if (parameter1 instanceof SingleVariableDeclaration) {
						variable1= ((SingleVariableDeclaration) parameter1).getName();
					} else if (parameter1 instanceof VariableDeclarationFragment) {
						variable1= ((VariableDeclarationFragment) parameter1).getName();
					} else {
						return true;
					}

					SimpleName variable2;
					if (parameter2 instanceof SingleVariableDeclaration) {
						variable2= ((SingleVariableDeclaration) parameter2).getName();
					} else if (parameter2 instanceof VariableDeclarationFragment) {
						variable2= ((VariableDeclarationFragment) parameter2).getName();
					} else {
						return true;
					}

					Expression bodyExpression= null;

					if (lambdaExpression.getBody() instanceof Block) {
						ReturnStatement returnStatement= ASTNodes.as((Block) lambdaExpression.getBody(),
								ReturnStatement.class);

						if (returnStatement == null) {
							return true;
						}

						bodyExpression= returnStatement.getExpression();
					} else if (lambdaExpression.getBody() instanceof Expression) {
						bodyExpression= (Expression) lambdaExpression.getBody();
					} else {
						return true;
					}

					if (isReturnedExpressionToRemove(variable1, variable2, bodyExpression, isForward)) {
						return maybeRemoveComparator(visitedIfRefactoringNeeded, list, comparatorToRemove, true);
					}
				}
			}

			return true;
		}

		private boolean isComparable(final ITypeBinding classBinding) {
			if ("java.lang.Comparable".equals(classBinding.getErasure().getQualifiedName())) { //$NON-NLS-1$
				return true;
			}

			ITypeBinding superClass= classBinding.getSuperclass();

			if (superClass != null && isComparable(superClass)) {
				return true;
			}

			for (ITypeBinding binding : classBinding.getInterfaces()) {
				if (isComparable(binding)) {
					return true;
				}
			}

			return false;
		}

		private boolean isClassToRemove(final ClassInstanceCreation classInstanceCreation, final boolean isForward) {
			AnonymousClassDeclaration anonymousClassDecl= classInstanceCreation.getAnonymousClassDeclaration();
			Type type= classInstanceCreation.getType();

			if (type != null
					&& type.resolveBinding() != null
					&& type.resolveBinding().getTypeArguments() != null
					&& type.resolveBinding().getTypeArguments().length == 1
					&& ASTNodes.hasType(type.resolveBinding(), Comparator.class.getCanonicalName())
					&& classInstanceCreation.arguments().isEmpty()
					&& anonymousClassDecl != null) {
				List<BodyDeclaration> bodies= anonymousClassDecl.bodyDeclarations();
				ITypeBinding typeArgument= type.resolveBinding().getTypeArguments()[0];

				if (bodies != null
						&& bodies.size() == 1
						&& typeArgument != null) {
					BodyDeclaration body= bodies.get(0);

					if (body instanceof MethodDeclaration) {
						MethodDeclaration methodDecl= (MethodDeclaration) body;
						ReturnStatement returnStatement= ASTNodes.as(methodDecl.getBody(), ReturnStatement.class);

						if (returnStatement != null
								&& returnStatement.getExpression() != null
								&& ASTNodes.usesGivenSignature(methodDecl, Comparator.class.getCanonicalName(), "compare", //$NON-NLS-1$
										typeArgument.getQualifiedName(),
										typeArgument.getQualifiedName())) {
							VariableDeclaration object1= (VariableDeclaration) methodDecl.parameters().get(0);
							VariableDeclaration object2= (VariableDeclaration) methodDecl.parameters().get(1);

							return isReturnedExpressionToRemove(object1.getName(), object2.getName(),
									returnStatement.getExpression(), isForward);
						}
					}
				}
			}

			return false;
		}

		private boolean isReturnedExpressionToRemove(final SimpleName name1, final SimpleName name2,
				final Expression returnExpression, final boolean isForward) {
			PrefixExpression negativeExpression= ASTNodes.as(returnExpression, PrefixExpression.class);

			if (negativeExpression != null && ASTNodes.hasOperator(negativeExpression, PrefixExpression.Operator.MINUS)) {
				return isReturnedExpressionToRemove(name1, name2, negativeExpression.getOperand(), !isForward);
			}

			MethodInvocation compareToMethod= ASTNodes.as(returnExpression, MethodInvocation.class);

			if (compareToMethod != null && compareToMethod.getExpression() != null) {
				ITypeBinding comparisonType= compareToMethod.getExpression().resolveTypeBinding();

				if (comparisonType != null) {
					List<Expression> arguments= compareToMethod.arguments();

					if (compareToMethod.getExpression() != null
							&& ASTNodes.usesGivenSignature(compareToMethod, comparisonType.getQualifiedName(), "compareTo", //$NON-NLS-1$
									comparisonType.getQualifiedName())) {
						return isRefactorComparisonToRefactor(name1, name2, compareToMethod.getExpression(),
								arguments.get(0), isForward);
					}

					String primitiveType= Bindings.getUnboxedTypeName(comparisonType.getQualifiedName());

					if (primitiveType != null
							&& ASTNodes.usesGivenSignature(compareToMethod, comparisonType.getQualifiedName(), "compare", //$NON-NLS-1$
									primitiveType, primitiveType)) {
						return isRefactorComparisonToRefactor(name1, name2, arguments.get(0), arguments.get(1), isForward);
					}
				}
			}

			return false;
		}

		private boolean isRefactorComparisonToRefactor(final SimpleName name1, final SimpleName name2,
				final Expression expression1, final Expression expression2,
				final boolean isForward) {
			if (isForward
					&& ASTNodes.isSameVariable(name1, expression1)
					&& ASTNodes.isSameVariable(name2, expression2)) {
				return true;
			}

			if (!isForward
					&& ASTNodes.isSameVariable(name1, expression2)
					&& ASTNodes.isSameVariable(name2, expression1)) {
				return true;
			}

			return false;
		}

		private boolean maybeRemoveComparator(
				final MethodInvocation visitedIfRefactoringNeeded,
				final Expression list,
				final Expression comparatorToRemove,
				final boolean isForward) {
			if (isForward) {
				fResult.add(new RedundantComparatorFixOperation(visitedIfRefactoringNeeded, list, comparatorToRemove));
				return false;
			}

			return true;
		}
	}

	public static class RedundantComparatorFixOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visitedIfRefactoringNeeded;
		private final Expression list;
		private final Expression comparator;

		public RedundantComparatorFixOperation(final MethodInvocation visitedIfRefactoringNeeded,
				final Expression list,
				final Expression comparator) {
			this.visitedIfRefactoringNeeded= visitedIfRefactoringNeeded;
			this.list= list;
			this.comparator= comparator;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.RedundantComparatorCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			if (visitedIfRefactoringNeeded != null) {
				String collectionsNameText= importRewrite.addImport(Collections.class.getCanonicalName());

				MethodInvocation sortMethod= ast.newMethodInvocation();
				sortMethod.setExpression(ASTNodeFactory.newName(ast, collectionsNameText));
				sortMethod.setName(ast.newSimpleName("sort")); //$NON-NLS-1$
				sortMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(list)));

				ASTNodes.replaceButKeepComment(rewrite, visitedIfRefactoringNeeded, sortMethod, group);
			} else {
				ASTNodes.removeButKeepComment(rewrite, comparator, group);
			}
		}
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<RedundantComparatorFixOperation> operations= new ArrayList<>();
		RedundantComparatorFinder finder= new RedundantComparatorFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new RedundantComparatorFixCore(FixMessages.RedundantComparatorFix_remove_comparator, compilationUnit, ops);
	}

	protected RedundantComparatorFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
