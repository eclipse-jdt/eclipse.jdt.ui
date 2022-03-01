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
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class ValueOfRatherThanInstantiationFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class ValueOfRatherThanInstantiationFinder extends ASTVisitor {
		private List<CompilationUnitRewriteOperation> fResult;

		public ValueOfRatherThanInstantiationFinder(List<CompilationUnitRewriteOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final ClassInstanceCreation visited) {
			ITypeBinding typeBinding= visited.getType().resolveBinding();
			List<Expression> args= visited.arguments();

			if (args.size() == 1) {
				Expression arg0= args.get(0);

				if (ASTNodes.hasType(typeBinding, Float.class.getCanonicalName())) {
					if (ASTNodes.hasType(arg0, double.class.getSimpleName())) {
						fResult.add(new ValueOfRatherThanInstantiationFloatWithValueOfFixOperation(visited, typeBinding, arg0));
						return false;
					}

					if (ASTNodes.hasType(arg0, Double.class.getCanonicalName())) {
						fResult.add(new ValueOfRatherThanInstantiationFloatWithFloatValueFixOperation(visited, arg0));
						return false;
					}
				}

				if (ASTNodes.hasType(typeBinding, Boolean.class.getCanonicalName(), Integer.class.getCanonicalName(), Long.class.getCanonicalName(), Double.class.getCanonicalName(),
						Short.class.getCanonicalName(), Float.class.getCanonicalName(), Byte.class.getCanonicalName(), Character.class.getCanonicalName())) {
					ITypeBinding destinationTypeBinding= ASTNodes.getTargetType(visited);

					if (destinationTypeBinding != null
							&& destinationTypeBinding.isPrimitive()
							&& !ASTNodes.hasType(arg0, String.class.getCanonicalName())) {
						fResult.add(new ValueOfRatherThanInstantiationWithTheSingleArgumentFixOperation(visited));
						return false;
					}

					if (JavaModelUtil.is50OrHigher(((CompilationUnit) visited.getRoot()).getJavaElement().getJavaProject())
							|| ASTNodes.hasType(typeBinding, String.class.getCanonicalName())) {
						fResult.add(new ValueOfRatherThanInstantiationWithValueOfFixOperation(visited, typeBinding, arg0));
						return false;
					}
				}
			}

			return true;
		}
	}

	public static class ValueOfRatherThanInstantiationFloatWithValueOfFixOperation extends CompilationUnitRewriteOperation {
		private final ClassInstanceCreation visited;
		private final ITypeBinding typeBinding;
		private final Expression arg0;

		public ValueOfRatherThanInstantiationFloatWithValueOfFixOperation(final ClassInstanceCreation visited, final ITypeBinding typeBinding, final Expression arg0) {
			this.visited= visited;
			this.typeBinding= typeBinding;
			this.arg0= arg0;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description_float_with_valueof, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			MethodInvocation valueOfMethod= ast.newMethodInvocation();
			valueOfMethod.setExpression(ASTNodeFactory.newName(ast, typeBinding.getName()));
			valueOfMethod.setName(ast.newSimpleName("valueOf")); //$NON-NLS-1$

			// remove all casts and parentheses from the argument
			Expression newArgument= arg0;
			while (true) {
				if (newArgument instanceof CastExpression) {
					newArgument= ((CastExpression) newArgument).getExpression();
					continue;
				}
				if (newArgument instanceof ParenthesizedExpression) {
					newArgument= ((ParenthesizedExpression) newArgument).getExpression();
					continue;
				}
				break;
			}

			if (!ASTNodes.hasType(newArgument, float.class.getSimpleName())) {
				CastExpression newCastExpression= ast.newCastExpression();
				newCastExpression.setType(ast.newPrimitiveType(PrimitiveType.FLOAT));

				Expression moveTarget= ASTNodes.createMoveTarget(rewrite, newArgument);

				if (newArgument.getNodeType() == ASTNode.INFIX_EXPRESSION || newArgument.getNodeType() == ASTNode.ASSIGNMENT) {
					// those expressions could be numeric and have less precedence than a cast, so need to put parens
					ParenthesizedExpression parens= ast.newParenthesizedExpression();
					parens.setExpression(moveTarget);
					moveTarget= parens;
				}

				newCastExpression.setExpression(moveTarget);
				newArgument= newCastExpression;
			}
			valueOfMethod.arguments().add(newArgument);



			ASTNodes.replaceButKeepComment(rewrite, visited, valueOfMethod, group);
		}
	}

	public static class ValueOfRatherThanInstantiationFloatWithFloatValueFixOperation extends CompilationUnitRewriteOperation {
		private final ClassInstanceCreation visited;
		private final Expression arg0;

		public ValueOfRatherThanInstantiationFloatWithFloatValueFixOperation(final ClassInstanceCreation visited, final Expression arg0) {
			this.visited= visited;
			this.arg0= arg0;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description_float_with_float_value, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			MethodInvocation floatValueMethod= ast.newMethodInvocation();
			floatValueMethod.setExpression(ASTNodes.createMoveTarget(rewrite, arg0));
			floatValueMethod.setName(ast.newSimpleName("floatValue")); //$NON-NLS-1$

			ASTNodes.replaceButKeepComment(rewrite, visited, floatValueMethod, group);
		}
	}

	public static class ValueOfRatherThanInstantiationWithTheSingleArgumentFixOperation extends CompilationUnitRewriteOperation {
		private final ClassInstanceCreation visited;

		public ValueOfRatherThanInstantiationWithTheSingleArgumentFixOperation(final ClassInstanceCreation visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description_single_argument, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodes.createMoveTarget(rewrite, (Expression) visited.arguments().get(0)), group);
		}
	}

	public static class ValueOfRatherThanInstantiationWithValueOfFixOperation extends CompilationUnitRewriteOperation {
		private final ClassInstanceCreation visited;
		private final ITypeBinding typeBinding;
		private final Expression arg0;

		public ValueOfRatherThanInstantiationWithValueOfFixOperation(final ClassInstanceCreation visited, final ITypeBinding typeBinding, final Expression arg0) {
			this.visited= visited;
			this.typeBinding= typeBinding;
			this.arg0= arg0;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description_valueof, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			MethodInvocation valueOfMethod= ast.newMethodInvocation();
			valueOfMethod.setExpression(ASTNodeFactory.newName(ast, typeBinding.getName()));
			valueOfMethod.setName(ast.newSimpleName("valueOf")); //$NON-NLS-1$
			valueOfMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(arg0)));

			ASTNodes.replaceButKeepComment(rewrite, visited, valueOfMethod, group);
		}
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		ValueOfRatherThanInstantiationFinder finder= new ValueOfRatherThanInstantiationFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperation[0]);
		return new ValueOfRatherThanInstantiationFixCore(FixMessages.ValueOfRatherThanInstantiationFix_description, compilationUnit, ops);
	}

	protected ValueOfRatherThanInstantiationFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
