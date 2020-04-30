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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
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
 * A fix that simplifies the lambda expression and the method reference syntax:
 * <ul>
 * <li>Parenthesis are not needed for a single untyped parameter,</li>
 * <li>Return statement is not needed for a single expression,</li>
 * <li>Brackets are not needed for a single statement,</li>
 * <li>A lambda expression can be replaced by a creation or a method reference in some cases.</li>
 * </ul>
 */
public class LambdaExpressionAndMethodRefCleanUp extends AbstractMultiFix {
	public LambdaExpressionAndMethodRefCleanUp() {
		this(Collections.emptyMap());
	}

	public LambdaExpressionAndMethodRefCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF)) {
			return new String[] { MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF)) {
			bld.append("someString -> someString.trim().toLowerCase();\n"); //$NON-NLS-1$
			bld.append("someString -> someString.trim().toLowerCase();\n"); //$NON-NLS-1$
			bld.append("someString -> (someString.trim().toLowerCase() + \"bar\");\n"); //$NON-NLS-1$
			bld.append("ArrayList::new;\n"); //$NON-NLS-1$
			bld.append("Date::getTime;\n"); //$NON-NLS-1$
		} else {
			bld.append("(someString) -> someString.trim().toLowerCase();\n"); //$NON-NLS-1$
			bld.append("someString -> {return someString.trim().toLowerCase();};\n"); //$NON-NLS-1$
			bld.append("someString -> {return someString.trim().toLowerCase() + \"bar\";};\n"); //$NON-NLS-1$
			bld.append("() -> new ArrayList<>();\n"); //$NON-NLS-1$
			bld.append("date -> date.getTime();\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF) || !JavaModelUtil.is18OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final LambdaExpression node) {
				if (node.hasParentheses() && node.parameters().size() == 1
						&& node.parameters().get(0) instanceof VariableDeclarationFragment) {
					rewriteOperations.add(new RemoveParamParenthesesOperation(node));
					return false;
				} else if (node.getBody() instanceof Block) {
					List<Statement> statements= ((Block) node.getBody()).statements();

					if (statements.size() == 1 && statements.get(0) instanceof ReturnStatement) {
						rewriteOperations.add(new RemoveReturnAndBracketsOperation(node, statements));
						return false;
					}
				} else if (node.getBody() instanceof ClassInstanceCreation) {
					ClassInstanceCreation ci= (ClassInstanceCreation) node.getBody();

					List<Expression> arguments= ci.arguments();
					if (node.parameters().size() == arguments.size()
							&& areSameIdentifiers(node, arguments)
							&& ci.getAnonymousClassDeclaration() == null) {
						rewriteOperations.add(new ReplaceByCreationReferenceOperation(node, ci));
						return false;
					}
				} else if (node.getBody() instanceof SuperMethodInvocation) {
					SuperMethodInvocation smi= (SuperMethodInvocation) node.getBody();
					List<Expression> arguments= smi.arguments();

					if (node.parameters().size() == arguments.size() && areSameIdentifiers(node, arguments)) {
						rewriteOperations.add(new ReplaceBySuperMethodReferenceOperation(node, smi));
						return false;
					}
				} else if (node.getBody() instanceof MethodInvocation) {
					MethodInvocation methodInvocation= (MethodInvocation) node.getBody();
					Expression calledExpression= methodInvocation.getExpression();
					List<Expression> arguments= methodInvocation.arguments();

					if (node.parameters().size() == arguments.size()) {
						if (!areSameIdentifiers(node, arguments)) {
							return true;
						}

						IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();

						if (Boolean.TRUE.equals(ASTNodes.isStatic(methodInvocation))) {
							if (methodBinding == null || methodBinding.getDeclaringClass() == null) {
								return true;
							}

							ITypeBinding calledType= methodBinding.getDeclaringClass();

							if (!arguments.isEmpty()) {
								String[] remainingParams= new String[arguments.size() - 1];

								for (int i= 0; i < arguments.size() - 1; i++) {
									ITypeBinding resolveTypeBinding= arguments.get(i + 1).resolveTypeBinding();

									if (resolveTypeBinding == null) {
										return true;
									}

									remainingParams[i]= resolveTypeBinding.getQualifiedName();
								}

								for (IMethodBinding declaredMethodBinding : calledType.getDeclaredMethods()) {
									if ((declaredMethodBinding.getModifiers() & Modifier.STATIC) == 0 && ASTNodes.usesGivenSignature(declaredMethodBinding,
											calledType.getQualifiedName(), methodInvocation.getName().getIdentifier(), remainingParams)) {
										return true;
									}
								}
							}

							rewriteOperations.add(new ReplaceByTypeReferenceOperation(node, methodInvocation, calledType));
							return false;
						}

						if (calledExpression == null) {
							if (methodBinding != null) {
								ITypeBinding calledType= methodBinding.getDeclaringClass();
								ITypeBinding enclosingType= Bindings.getBindingOfParentType(node);

								if (calledType != null && Bindings.isSuperType(calledType, enclosingType)) {
									rewriteOperations.add(new ReplaceByMethodReferenceOperation(node, methodInvocation));
									return false;
								}
							}
						} else if (calledExpression instanceof StringLiteral || calledExpression instanceof NumberLiteral
								|| calledExpression instanceof ThisExpression) {
							rewriteOperations.add(new ReplaceByMethodReferenceOperation(node, methodInvocation));
							return false;
						} else if (calledExpression instanceof FieldAccess) {
							FieldAccess fieldAccess= (FieldAccess) calledExpression;

							if (fieldAccess.resolveFieldBinding() != null && fieldAccess.resolveFieldBinding().isEffectivelyFinal()) {
								rewriteOperations.add(new ReplaceByMethodReferenceOperation(node, methodInvocation));
								return false;
							}
						} else if (calledExpression instanceof SuperFieldAccess) {
							SuperFieldAccess fieldAccess= (SuperFieldAccess) calledExpression;

							if (fieldAccess.resolveFieldBinding() != null && fieldAccess.resolveFieldBinding().isEffectivelyFinal()) {
								rewriteOperations.add(new ReplaceByMethodReferenceOperation(node, methodInvocation));
								return false;
							}
						}
					} else if (calledExpression instanceof SimpleName && node.parameters().size() == arguments.size() + 1) {
						SimpleName calledObject= (SimpleName) calledExpression;

						if (isSameIdentifier(node, 0, calledObject)) {
							for (int i= 0; i < arguments.size(); i++) {
								ASTNode expression= ASTNodes.getUnparenthesedExpression(arguments.get(i));

								if (!(expression instanceof SimpleName) || !isSameIdentifier(node, i + 1, (SimpleName) expression)) {
									return true;
								}
							}

							ITypeBinding clazz= null;
							if (calledExpression.resolveTypeBinding() != null) {
								clazz= calledExpression.resolveTypeBinding();
							} else if (methodInvocation.resolveMethodBinding() != null && methodInvocation.resolveMethodBinding().getDeclaringClass() != null) {
								clazz= methodInvocation.resolveMethodBinding().getDeclaringClass();
							} else {
								return true;
							}

							String[] cumulativeParams= new String[arguments.size() + 1];
							cumulativeParams[0]= clazz.getQualifiedName();

							for (int i= 0; i < arguments.size(); i++) {
								ITypeBinding resolveTypeBinding= arguments.get(i).resolveTypeBinding();

								if (resolveTypeBinding == null) {
									return true;
								}

								cumulativeParams[i + 1]= resolveTypeBinding.getQualifiedName();
							}

							for (IMethodBinding declaredMethodBinding : clazz.getDeclaredMethods()) {
								if ((declaredMethodBinding.getModifiers() & Modifier.STATIC) > 0 && ASTNodes.usesGivenSignature(declaredMethodBinding,
										clazz.getQualifiedName(), methodInvocation.getName().getIdentifier(), cumulativeParams)) {
									return true;
								}
							}

							rewriteOperations.add(new ReplaceByTypeReferenceOperation(node, methodInvocation, clazz));
							return false;
						}
					}
				}

				return true;
			}

			/**
			 * In order to make parameters implicit, those ones should be the same in the same
			 * order.
			 *
			 * @param node the lambda expression
			 * @param arguments The arguments in the expression
			 * @return true if the parameters are obvious
			 */
			private boolean areSameIdentifiers(LambdaExpression node, List<Expression> arguments) {
				for (int i= 0; i < node.parameters().size(); i++) {
					Expression expression= ASTNodes.getUnparenthesedExpression(arguments.get(i));

					if (!(expression instanceof SimpleName) || !isSameIdentifier(node, i, (SimpleName) expression)) {
						return false;
					}
				}

				return true;
			}

			private boolean isSameIdentifier(final LambdaExpression node, final int i, final SimpleName argument) {
				Object param0= node.parameters().get(i);

				if (param0 instanceof VariableDeclarationFragment) {
					VariableDeclarationFragment vdf= (VariableDeclarationFragment) param0;
					return vdf.getName().getIdentifier().equals(argument.getIdentifier());
				}

				return false;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[rewriteOperations.size()]));
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class RemoveParamParenthesesOperation extends CompilationUnitRewriteOperation {
		private final LambdaExpression node;

		public RemoveParamParenthesesOperation(final LambdaExpression node) {
			this.node= node;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			LambdaExpression copyOfLambdaExpression= ast.newLambdaExpression();
			ASTNode copyOfParameter= rewrite.createCopyTarget((ASTNode) node.parameters().get(0));
			copyOfLambdaExpression.parameters().add(copyOfParameter);
			copyOfLambdaExpression.setBody(rewrite.createCopyTarget(node.getBody()));
			copyOfLambdaExpression.setParentheses(false);
			rewrite.replace(node, copyOfLambdaExpression, null);
		}
	}

	private static class RemoveReturnAndBracketsOperation extends CompilationUnitRewriteOperation {
		private final LambdaExpression node;

		private final List<Statement> statements;

		public RemoveReturnAndBracketsOperation(final LambdaExpression node, final List<Statement> statements) {
			this.node= node;
			this.statements= statements;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			ReturnStatement returnStatement= (ReturnStatement) statements.get(0);
			Expression copyOfExpression= (Expression) rewrite.createCopyTarget(returnStatement.getExpression());
			rewrite.replace(node.getBody(), ASTNodeFactory.parenthesizeIfNeeded(ast, copyOfExpression), null);
		}
	}

	private static class ReplaceByCreationReferenceOperation extends CompilationUnitRewriteOperation {
		private final LambdaExpression node;

		private final ClassInstanceCreation classInstanceCreation;

		public ReplaceByCreationReferenceOperation(final LambdaExpression node, final ClassInstanceCreation classInstanceCreation) {
			this.node= node;
			this.classInstanceCreation= classInstanceCreation;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			CreationReference creationRef= ast.newCreationReference();
			creationRef.setType(copyType(cuRewrite, ast, classInstanceCreation, classInstanceCreation.resolveTypeBinding()));
			rewrite.replace(node, creationRef, null);
		}
	}

	private static class ReplaceBySuperMethodReferenceOperation extends CompilationUnitRewriteOperation {
		private final LambdaExpression node;

		private final SuperMethodInvocation superMethodInvocation;

		public ReplaceBySuperMethodReferenceOperation(final LambdaExpression node, final SuperMethodInvocation superMethodInvocation) {
			this.node= node;
			this.superMethodInvocation= superMethodInvocation;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			SuperMethodReference creationRef= ast.newSuperMethodReference();
			creationRef.setName((SimpleName) rewrite.createCopyTarget(superMethodInvocation.getName()));
			rewrite.replace(node, creationRef, null);
		}
	}

	private static class ReplaceByTypeReferenceOperation extends CompilationUnitRewriteOperation {
		private final LambdaExpression node;
		private final MethodInvocation methodInvocation;

		private final ITypeBinding type;

		public ReplaceByTypeReferenceOperation(LambdaExpression node, MethodInvocation methodInvocation, ITypeBinding type) {
			this.node= node;
			this.methodInvocation= methodInvocation;
			this.type= type;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			TypeMethodReference typeMethodRef= ast.newTypeMethodReference();

			typeMethodRef.setType(copyType(cuRewrite, ast, methodInvocation, type));
			typeMethodRef.setName((SimpleName) rewrite.createCopyTarget(methodInvocation.getName()));
			rewrite.replace(node, typeMethodRef, null);
		}
	}

	private static class ReplaceByMethodReferenceOperation extends CompilationUnitRewriteOperation {
		private final LambdaExpression node;

		private final MethodInvocation methodInvocation;

		public ReplaceByMethodReferenceOperation(LambdaExpression node, MethodInvocation methodInvocation) {
			this.node= node;
			this.methodInvocation= methodInvocation;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			ExpressionMethodReference typeMethodRef= ast.newExpressionMethodReference();

			if (methodInvocation.getExpression() != null) {
				typeMethodRef.setExpression((Expression) rewrite.createCopyTarget(methodInvocation.getExpression()));
			} else {
				typeMethodRef.setExpression(ast.newThisExpression());
			}

			typeMethodRef.setName((SimpleName) rewrite.createCopyTarget(methodInvocation.getName()));
			rewrite.replace(node, typeMethodRef, null);
		}
	}

	private static Type copyType(final CompilationUnitRewrite cuRewrite, final AST ast, final ASTNode node, final ITypeBinding typeBinding) {
		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		ImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(node, importRewrite);
		ITypeBinding modifiedType;

		if (typeBinding.getTypeParameters().length == 0) {
			modifiedType= typeBinding.getErasure();
		} else {
			modifiedType= typeBinding;
		}

		return ASTNodeFactory.newCreationType(ast, modifiedType, importRewrite, importContext);
	}
}