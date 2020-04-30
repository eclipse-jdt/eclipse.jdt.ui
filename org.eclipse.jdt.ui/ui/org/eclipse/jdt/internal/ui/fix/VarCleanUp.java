/*******************************************************************************
 * Copyright (c) 2020 Fabrice TIERCELIN and others.
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
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that uses the Local variable type inference:
 * <ul>
 * <li>As of Java 10, if a variable is initialized by an explicit type value, it can be declared
 * using the <code>var</code> keyword.</li>
 * </ul>
 */
public class VarCleanUp extends AbstractMultiFix {
	public VarCleanUp() {
		this(Collections.emptyMap());
	}

	public VarCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.USE_VAR);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_VAR)) {
			return new String[] { MultiFixMessages.VarCleanUp_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		if (isEnabled(CleanUpConstants.USE_VAR)) {
			bld.append("var number = 0;\n"); //$NON-NLS-1$
			bld.append("var list = new ArrayList<String>();\n"); //$NON-NLS-1$
			bld.append("var map = new HashMap<Integer, String>();\n"); //$NON-NLS-1$
		} else {
			bld.append("int number = 0;\n"); //$NON-NLS-1$
			bld.append("ArrayList<String> list = new ArrayList<String>();\n"); //$NON-NLS-1$
			bld.append("HashMap<Integer, String> map = new HashMap<>();\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.USE_VAR) || !JavaModelUtil.is10OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final VariableDeclarationStatement node) {
				if (node.fragments().size() != 1) {
					return true;
				}

				VariableDeclarationFragment fragment= (VariableDeclarationFragment) node.fragments().get(0);

				return maybeUseVar(node.getType(), fragment.getInitializer(), fragment.getExtraDimensions());
			}

			@Override
			public boolean visit(final VariableDeclarationExpression node) {
				if (node.fragments().size() != 1) {
					return true;
				}

				VariableDeclarationFragment fragment= (VariableDeclarationFragment) node.fragments().get(0);

				return maybeUseVar(node.getType(), fragment.getInitializer(), fragment.getExtraDimensions());
			}

			@Override
			public boolean visit(final SingleVariableDeclaration node) {
				return maybeUseVar(node.getType(), node.getInitializer(), node.getExtraDimensions());
			}

			private boolean maybeUseVar(final Type type, final Expression initializer, final int extraDimensions) {
				if (type.isVar() || initializer == null || initializer.resolveTypeBinding() == null || type.resolveBinding() == null
						|| extraDimensions > 0) {
					return true;
				}

				ITypeBinding variableType= type.resolveBinding();
				ITypeBinding initializerType= initializer.resolveTypeBinding();

				if (variableType != null
						&& variableType.isParameterizedType() == initializerType.isParameterizedType()) {
					if (Objects.equals(variableType, initializerType)) {
						ClassInstanceCreation classInstanceCreation= ASTNodes.as(initializer, ClassInstanceCreation.class);
						CastExpression castExpression= ASTNodes.as(initializer, CastExpression.class);
						MethodInvocation methodInvocation= ASTNodes.as(initializer, MethodInvocation.class);
						LambdaExpression lambdaExpression= ASTNodes.as(initializer, LambdaExpression.class);
						Expression expression= ASTNodes.as(initializer, Expression.class);

						if (!variableType.isParameterizedType()
								|| (classInstanceCreation != null
										&& classInstanceCreation.getType().isParameterizedType()
										&& classInstanceCreation.getType().resolveBinding() != null
										&& Objects.equals(variableType.getTypeArguments(), classInstanceCreation.getType().resolveBinding().getTypeArguments())
										&& !((ParameterizedType) classInstanceCreation.getType()).typeArguments().isEmpty())
								|| (castExpression != null
										&& castExpression.getType().isParameterizedType()
										&& castExpression.getType().resolveBinding() != null
										&& variableType.getTypeArguments().length == ((ParameterizedType) castExpression.getType()).typeArguments().size()
										&& Objects.equals(variableType.getTypeArguments(), castExpression.getType().resolveBinding().getTypeArguments()))
								|| (methodInvocation != null
										&& methodInvocation.resolveMethodBinding() != null
										&& methodInvocation.resolveMethodBinding().getReturnType().isParameterizedType()
										&& Objects.equals(variableType.getTypeArguments(), methodInvocation.resolveMethodBinding().getReturnType().getTypeArguments()))
								|| (classInstanceCreation == null
										&& castExpression == null
										&& methodInvocation == null
										&& lambdaExpression == null
										&& expression != null
										&& expression.resolveTypeBinding() != null
										&& expression.resolveTypeBinding().isParameterizedType()
										&& Objects.equals(variableType.getTypeArguments(), expression.resolveTypeBinding().getTypeArguments()))) {
							rewriteOperations.add(new VarOperation(type));
							return false;
						} else if (variableType.isParameterizedType()
								&& classInstanceCreation != null
								&& classInstanceCreation.getType().isParameterizedType()
								&& ((ParameterizedType) classInstanceCreation.getType()).typeArguments().isEmpty()) {
							rewriteOperations.add(new VarOperation(type, classInstanceCreation));
							return false;
						}
					} else {
						NumberLiteral literal= ASTNodes.as(initializer, NumberLiteral.class);

				        if (literal != null && (literal.getToken().matches(".*[^lLdDfF]") || literal.getToken().matches("0x.*[^lL]"))) { //$NON-NLS-1$ //$NON-NLS-2$
				            if (ASTNodes.hasType(variableType, long.class.getSimpleName())) {
				            	rewriteOperations.add(new VarOperation(type, literal, Character.valueOf('L')));
				                return false;
				            }

				            if (ASTNodes.hasType(variableType, float.class.getSimpleName())) {
				            	rewriteOperations.add(new VarOperation(type, literal, Character.valueOf('F')));
				                return false;
				            }

				            if (ASTNodes.hasType(variableType, double.class.getSimpleName())) {
				            	rewriteOperations.add(new VarOperation(type, literal, Character.valueOf('D')));
				                return false;
				            }
				        }
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.VarCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[rewriteOperations.size()]));
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private class VarOperation extends CompilationUnitRewriteOperation {
		private final Type node;
		private final ClassInstanceCreation classInstanceCreation;
		private final NumberLiteral literal;
		private final Character postfix;

		public VarOperation(final Type node) {
			this(node, null, null, null);
		}

		public VarOperation(final Type node, final ClassInstanceCreation classInstanceCreation) {
			this(node, classInstanceCreation, null, null);
		}

		public VarOperation(final Type node, final NumberLiteral literal, final Character postfix) {
			this(node, null, literal, postfix);
		}

		public VarOperation(final Type node, final ClassInstanceCreation classInstanceCreation, final NumberLiteral literal, final Character postfix) {
			this.node= node;
			this.classInstanceCreation= classInstanceCreation;
			this.literal= literal;
			this.postfix= postfix;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			if (classInstanceCreation != null) {
				rewrite.replace(classInstanceCreation.getType(), rewrite.createCopyTarget(node), null);
			} else if (literal != null) {
				rewrite.replace(literal, ast.newNumberLiteral(literal.getToken() + postfix), null);
			}

			rewrite.replace(node, ast.newSimpleType(ast.newSimpleName("var")), null); //$NON-NLS-1$
		}
	}
}
