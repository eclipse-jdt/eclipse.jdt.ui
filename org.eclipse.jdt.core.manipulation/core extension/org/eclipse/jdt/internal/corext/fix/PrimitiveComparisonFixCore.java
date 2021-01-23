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
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class PrimitiveComparisonFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class PrimitiveComparisonFinder extends ASTVisitor {
		private List<PrimitiveComparisonFixOperation> fResult;

		public PrimitiveComparisonFinder(List<PrimitiveComparisonFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final MethodInvocation visited) {
			if (visited.getExpression() != null
					&& visited.arguments().size() == 1) {
				Class<?>[] wrapperClasses= { Integer.class, Boolean.class, Long.class, Double.class, Character.class, Float.class, Short.class, Byte.class };

				for (Class<?> wrapperClass : wrapperClasses) {
					String canonicalName= wrapperClass.getCanonicalName();
					String primitiveName= Bindings.getUnboxedTypeName(canonicalName);

					if (ASTNodes.isPrimitive((Expression) visited.arguments().get(0), primitiveName)
							&& ASTNodes.usesGivenSignature(visited, canonicalName, "compareTo", canonicalName)) { //$NON-NLS-1$
						MethodInvocation methodInvocation = ASTNodes.as(visited.getExpression(), MethodInvocation.class);

						if (methodInvocation != null
								&& ASTNodes.usesGivenSignature(methodInvocation, canonicalName, "valueOf", primitiveName) //$NON-NLS-1$
								&& ASTNodes.isPrimitive((Expression) methodInvocation.arguments().get(0), primitiveName)) {
							fResult.add(new PrimitiveComparisonFixOperation(visited, (Expression) methodInvocation.arguments().get(0), wrapperClass));
							return false;
						}

						ClassInstanceCreation classInstanceCreation = ASTNodes.as(visited.getExpression(), ClassInstanceCreation.class);

						if (classInstanceCreation != null
								&& ASTNodes.hasType(classInstanceCreation.getType().resolveBinding(), canonicalName)
								&& ASTNodes.isPrimitive((Expression) classInstanceCreation.arguments().get(0), primitiveName)) {
							fResult.add(new PrimitiveComparisonFixOperation(visited, (Expression) classInstanceCreation.arguments().get(0), wrapperClass));
							return false;
						}

						CastExpression castExpression = ASTNodes.as(visited.getExpression(), CastExpression.class);

						if (castExpression != null
								&& ASTNodes.hasType(castExpression.getType().resolveBinding(), canonicalName)
								&& ASTNodes.isPrimitive(castExpression.getExpression(), primitiveName)) {
							fResult.add(new PrimitiveComparisonFixOperation(visited, castExpression.getExpression(), wrapperClass));
							return false;
						}

						return true;
					}
				}
			}

			return true;
		}
	}

	public static class PrimitiveComparisonFixOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visited;
		private final Expression primitiveValue;
		private final Class<?> wrapperClass;

		public PrimitiveComparisonFixOperation(final MethodInvocation visited, final Expression primitiveValue, final Class<?> wrapperClass) {
			this.visited= visited;
			this.primitiveValue= primitiveValue;
			this.wrapperClass= wrapperClass;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PrimitiveComparisonCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});


			MethodInvocation compareMethod= ast.newMethodInvocation();
			compareMethod.setExpression(ast.newSimpleName(wrapperClass.getSimpleName()));
			compareMethod.setName(ast.newSimpleName("compare")); //$NON-NLS-1$
			compareMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(primitiveValue)));
			compareMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression((Expression) visited.arguments().get(0))));

			ASTNodes.replaceButKeepComment(rewrite, visited, compareMethod, group);
		}
	}


	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<PrimitiveComparisonFixOperation> operations= new ArrayList<>();
		PrimitiveComparisonFinder finder= new PrimitiveComparisonFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new PrimitiveComparisonFixCore(FixMessages.PrimitiveComparisonFix_convert_compareTo_to_primitive_comparison, compilationUnit, ops);
	}

	protected PrimitiveComparisonFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
