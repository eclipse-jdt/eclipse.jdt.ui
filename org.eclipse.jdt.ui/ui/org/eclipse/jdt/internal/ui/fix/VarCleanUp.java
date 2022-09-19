/*******************************************************************************
 * Copyright (c) 2020, 2021 Fabrice TIERCELIN and others.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
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
		if (isEnabled(CleanUpConstants.USE_VAR)) {
			return "" //$NON-NLS-1$
					+ "var number = 0;\n" //$NON-NLS-1$
					+ "var list = new ArrayList<String>();\n" //$NON-NLS-1$
					+ "var map = new HashMap<Integer, String>();\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "int number = 0;\n" //$NON-NLS-1$
				+ "ArrayList<String> list = new ArrayList<String>();\n" //$NON-NLS-1$
				+ "HashMap<Integer, String> map = new HashMap<>();\n"; //$NON-NLS-1$
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
				if (type.isVar()
						|| initializer == null
						|| initializer.resolveTypeBinding() == null
						|| type.resolveBinding() == null
						|| extraDimensions > 0
						|| initializer instanceof ArrayInitializer) {
					if (JavaModelUtil.is11OrHigher(unit.getJavaElement().getJavaProject())
							&& !type.isVar()
							&& initializer == null
							&& type.resolveBinding() != null
							&& extraDimensions == 0
							&& type.getParent() instanceof SingleVariableDeclaration
							&& type.getParent().getParent() instanceof LambdaExpression
							&& type.getParent().getLocationInParent() == LambdaExpression.PARAMETERS_PROPERTY) {
						LambdaExpression lambda= (LambdaExpression) type.getParent().getParent();
						ASTNode lambdaParent= lambda.getParent();

						if (lambdaParent instanceof MethodInvocation) {
							MethodInvocation methodInvocation= (MethodInvocation) lambdaParent;
							List<Expression> args= methodInvocation.arguments();
							IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
							if (checkForWildCard(lambda, args, methodBinding)) {
								return true;
							}
						} else if (lambdaParent instanceof ClassInstanceCreation) {
							ClassInstanceCreation classInstance= (ClassInstanceCreation) lambdaParent;
							List<Expression> args= classInstance.arguments();
							IMethodBinding methodBinding= classInstance.resolveConstructorBinding();
							if (checkForWildCard(lambda, args, methodBinding)) {
								return true;
							}
						} else if (lambdaParent instanceof SuperMethodInvocation) {
							SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) lambdaParent;
							List<Expression> args= superMethodInvocation.arguments();
							IMethodBinding methodBinding= superMethodInvocation.resolveMethodBinding();
							if (checkForWildCard(lambda, args, methodBinding)) {
								return true;
							}
						} else if (lambdaParent instanceof SuperConstructorInvocation) {
							SuperConstructorInvocation superConstructorInvocation= (SuperConstructorInvocation) lambdaParent;
							List<Expression> args= superConstructorInvocation.arguments();
							IMethodBinding methodBinding= superConstructorInvocation.resolveConstructorBinding();
							if (checkForWildCard(lambda, args, methodBinding)) {
								return true;
							}
						} else if (lambdaParent instanceof VariableDeclarationFragment) {
							VariableDeclarationStatement statement= ASTNodes.getFirstAncestorOrNull(lambdaParent, VariableDeclarationStatement.class);
							FieldDeclaration fieldDeclaration= ASTNodes.getFirstAncestorOrNull(lambdaParent, FieldDeclaration.class);
							Type statementType= null;

							if (statement != null) {
								statementType= statement.getType();
							} else if (fieldDeclaration != null) {
								statementType= fieldDeclaration.getType();
							}

							if (statementType == null) {
								return true;
							}

							if (statementType.isParameterizedType()) {
								ParameterizedType parameterizedType= (ParameterizedType) statementType;
								List<Type> typeArgs= parameterizedType.typeArguments();

								for (Type typeArg : typeArgs) {
									if (typeArg.isWildcardType()) {
										return true;
									}
								}
							}
						}

						rewriteOperations.add(new VarOperation(type));
						return false;
					}

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
						SuperMethodInvocation superMethodInvocation= ASTNodes.as(initializer, SuperMethodInvocation.class);
						LambdaExpression lambdaExpression= ASTNodes.as(initializer, LambdaExpression.class);
						Expression expression= ASTNodes.as(initializer, Expression.class);
						MethodReference methodReference= ASTNodes.as(initializer, MethodReference.class);

						if (!variableType.isParameterizedType() && lambdaExpression == null
								|| (classInstanceCreation != null
										&& classInstanceCreation.getType().isParameterizedType()
										&& classInstanceCreation.getType().resolveBinding() != null
										&& Arrays.equals(variableType.getTypeArguments(), classInstanceCreation.getType().resolveBinding().getTypeArguments())
										&& !((ParameterizedType) classInstanceCreation.getType()).typeArguments().isEmpty())
								|| (castExpression != null
										&& castExpression.getType().isParameterizedType()
										&& castExpression.getType().resolveBinding() != null
										&& variableType.getTypeArguments().length == ((ParameterizedType) castExpression.getType()).typeArguments().size()
										&& Arrays.equals(variableType.getTypeArguments(), castExpression.getType().resolveBinding().getTypeArguments()))
								|| (methodInvocation != null
										&& methodInvocation.resolveMethodBinding() != null
										&& methodInvocation.resolveMethodBinding().getReturnType().isParameterizedType()
										&& !methodInvocation.resolveMethodBinding().isParameterizedMethod()
										&& Arrays.equals(variableType.getTypeArguments(), methodInvocation.resolveMethodBinding().getReturnType().getTypeArguments()))
								|| (superMethodInvocation != null
										&& superMethodInvocation.resolveMethodBinding() != null
										&& superMethodInvocation.resolveMethodBinding().getReturnType().isParameterizedType()
										&& !superMethodInvocation.resolveMethodBinding().isParameterizedMethod()
										&& Arrays.equals(variableType.getTypeArguments(), superMethodInvocation.resolveMethodBinding().getReturnType().getTypeArguments()))
								|| (classInstanceCreation == null
										&& castExpression == null
										&& methodInvocation == null
										&& superMethodInvocation == null
										&& lambdaExpression == null
										&& methodReference == null
										&& expression != null
										&& expression.resolveTypeBinding() != null
										&& expression.resolveTypeBinding().isParameterizedType()
										&& Arrays.equals(variableType.getTypeArguments(), expression.resolveTypeBinding().getTypeArguments()))) {
							rewriteOperations.add(new VarOperation(type));
							return false;
						} else if (variableType.isParameterizedType()
								&& methodReference == null
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
				            	rewriteOperations.add(new VarOperation(type, literal, 'L'));
				                return false;
				            }

				            if (ASTNodes.hasType(variableType, float.class.getSimpleName())) {
				            	rewriteOperations.add(new VarOperation(type, literal, 'F'));
				                return false;
				            }

				            if (ASTNodes.hasType(variableType, double.class.getSimpleName())) {
				            	rewriteOperations.add(new VarOperation(type, literal, 'D'));
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
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	private boolean checkForWildCard(LambdaExpression lambda, List<Expression> args, IMethodBinding methodBinding) {
		int index= -1;

		for (int i= 0; i < args.size(); ++i) {
			if (args.get(i) == lambda) {
				index= i;
				break;
			}
		}

		if (index < 0) {
			return true;
		}

		if (methodBinding == null) {
			return true;
		}

		ITypeBinding lambdaParamType= methodBinding.getParameterTypes()[index];
		ITypeBinding[] typeArgs= lambdaParamType.getTypeArguments();

		for (ITypeBinding typeArg : typeArgs) {
			if (typeArg.isWildcardType()) {
				return true;
			}
		}
		return false;
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
			TextEditGroup group= createTextEditGroup(MultiFixMessages.VarCleanUp_description, cuRewrite);

			if (classInstanceCreation != null) {
				Type node1= classInstanceCreation.getType();
				ASTNode replacement= rewrite.createCopyTarget(node);
				ASTNodes.replaceButKeepComment(rewrite, node1, replacement, group);
			} else if (literal != null) {
				NumberLiteral replacement= ast.newNumberLiteral(literal.getToken() + postfix);
				ASTNodes.replaceButKeepComment(rewrite, literal, replacement, group);
			}

			SimpleType replacement= ast.newSimpleType(ast.newSimpleName("var")); //$NON-NLS-1$
			ASTNodes.replaceButKeepComment(rewrite, node, replacement, group);
		}
	}
}
