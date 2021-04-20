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

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
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

	public LambdaExpressionAndMethodRefCleanUp(final Map<String, String> options) {
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
		if (isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF)) {
			return "" //$NON-NLS-1$
					+ "someString -> someString.trim().toLowerCase();\n" //$NON-NLS-1$
					+ "someString -> someString.trim().toLowerCase();\n" //$NON-NLS-1$
					+ "someString -> (someString.trim().toLowerCase() + \"bar\");\n" //$NON-NLS-1$
					+ "ArrayList::new;\n" //$NON-NLS-1$
					+ "Date::getTime;\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "(someString) -> someString.trim().toLowerCase();\n" //$NON-NLS-1$
				+ "someString -> {return someString.trim().toLowerCase();};\n" //$NON-NLS-1$
				+ "someString -> {return someString.trim().toLowerCase() + \"bar\";};\n" //$NON-NLS-1$
				+ "() -> new ArrayList<>();\n" //$NON-NLS-1$
				+ "date -> date.getTime();\n"; //$NON-NLS-1$
	}

	private enum ActionType { DO_NOTHING, REMOVE_RETURN, CLASS_INSTANCE_REF, TYPE_REF, SUPER_METHOD_REF, METHOD_REF }

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF) || !JavaModelUtil.is1d8OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final LambdaExpression visited) {
				ActionType actionType= ActionType.DO_NOTHING;
				ITypeBinding classBinding= null;

				boolean removeParamParentheses= hasToRemoveParamParentheses(visited);
				Expression bodyExpression= null;

				if (visited.getBody() instanceof Block) {
					ReturnStatement returnStatement= ASTNodes.as((Block) visited.getBody(), ReturnStatement.class);

					if (returnStatement != null) {
						bodyExpression= returnStatement.getExpression();
						actionType= ActionType.REMOVE_RETURN;
					}
				} else if (visited.getBody() instanceof Expression) {
					bodyExpression= (Expression) visited.getBody();
				}

				if (bodyExpression instanceof ClassInstanceCreation) {
					ClassInstanceCreation classInstanceCreation= (ClassInstanceCreation) bodyExpression;
					List<Expression> arguments= classInstanceCreation.arguments();

					if (visited.parameters().size() == arguments.size()
							&& areSameIdentifiers(visited, arguments)
							&& classInstanceCreation.getAnonymousClassDeclaration() == null) {
						actionType= ActionType.CLASS_INSTANCE_REF;
					}
				} else if (bodyExpression instanceof SuperMethodInvocation) {
					SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) bodyExpression;
					List<Expression> arguments= superMethodInvocation.arguments();

					if (visited.parameters().size() == arguments.size() && areSameIdentifiers(visited, arguments)) {
						actionType= ActionType.SUPER_METHOD_REF;
					}
				} else if (bodyExpression instanceof MethodInvocation) {
					MethodInvocation methodInvocation= (MethodInvocation) bodyExpression;
					Expression calledExpression= methodInvocation.getExpression();
					List<Expression> arguments= methodInvocation.arguments();

					if (visited.parameters().size() == arguments.size()) {
						if (areSameIdentifiers(visited, arguments)) {
							IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
							ITypeBinding calledType= null;

							if (methodBinding != null) {
								calledType= methodBinding.getDeclaringClass();
							} else if (calledExpression != null) {
								calledType= calledExpression.resolveTypeBinding();
							} else {
								// For an unknown reason, MethodInvocation.resolveMethodBinding() seems to fail when the method is defined in the class we are
								AbstractTypeDeclaration enclosingType= ASTNodes.getTypedAncestor(visited, AbstractTypeDeclaration.class);

								if (enclosingType != null) {
									ITypeBinding enclosingTypeBinding= enclosingType.resolveBinding();

									if (enclosingTypeBinding != null) {
										List<Type> argumentTypes= methodInvocation.typeArguments();
										String[] parameterTypeNames= new String[methodInvocation.arguments().size()];

										for (int i= 0; i < argumentTypes.size(); i++) {
											Type argumentType= argumentTypes.get(i);

											if (argumentType.resolveBinding() == null) {
												return true;
											}

											parameterTypeNames[i]= argumentType.resolveBinding().getQualifiedName();
										}

										for (IMethodBinding declaredMethods : enclosingTypeBinding.getDeclaredMethods()) {
											if (ASTNodes.usesGivenSignature(declaredMethods, enclosingTypeBinding.getQualifiedName(), methodInvocation.getName().getIdentifier(), parameterTypeNames)) {
												calledType= enclosingTypeBinding;
												break;
											}
										}
									}
								}
							}

							if (Boolean.TRUE.equals(ASTNodes.isStatic(methodInvocation))
									&& calledType != null) {
								boolean valid= true;

								if (!arguments.isEmpty()
										&& arguments.get(0).resolveTypeBinding() != null
										&& arguments.get(0).resolveTypeBinding().isSubTypeCompatible(calledType)) {
									String[] remainingParams= new String[arguments.size() - 1];

									for (int i= 0; i < arguments.size() - 1; i++) {
										ITypeBinding resolveTypeBinding= arguments.get(i + 1).resolveTypeBinding();

										if (resolveTypeBinding == null) {
											valid= false;
											break;
										}

										remainingParams[i]= resolveTypeBinding.getQualifiedName();
									}

									if (valid) {
										for (IMethodBinding declaredMethodBinding : calledType.getDeclaredMethods()) {
											if (!Modifier.isStatic(declaredMethodBinding.getModifiers())
													&& ASTNodes.usesGivenSignature(declaredMethodBinding, calledType.getQualifiedName(), methodInvocation.getName().getIdentifier(), remainingParams)) {
												valid= false;
												break;
											}
										}
									}
								}

								if (valid) {
									actionType= ActionType.TYPE_REF;
									classBinding= calledType;
								}
							}

							if (actionType != ActionType.TYPE_REF) {
								if (calledExpression == null) {
									if (calledType != null) {
										ITypeBinding enclosingType= Bindings.getBindingOfParentType(visited);

										if (enclosingType != null && Bindings.isSuperType(calledType, enclosingType)) {
											actionType= ActionType.METHOD_REF;
										}
									}
								} else if (calledExpression instanceof StringLiteral
										|| calledExpression instanceof NumberLiteral
										|| calledExpression instanceof ThisExpression) {
									actionType= ActionType.METHOD_REF;
								} else if (calledExpression instanceof FieldAccess) {
									FieldAccess fieldAccess= (FieldAccess) calledExpression;

									if (fieldAccess.resolveFieldBinding() != null && fieldAccess.resolveFieldBinding().isEffectivelyFinal()) {
										actionType= ActionType.METHOD_REF;
									}
								} else if (calledExpression instanceof SuperFieldAccess) {
									SuperFieldAccess fieldAccess= (SuperFieldAccess) calledExpression;

									if (fieldAccess.resolveFieldBinding() != null && fieldAccess.resolveFieldBinding().isEffectivelyFinal()) {
										actionType= ActionType.METHOD_REF;
									}
								}
							}
						}
					} else if (calledExpression instanceof SimpleName && visited.parameters().size() == arguments.size() + 1) {
						SimpleName calledObject= (SimpleName) calledExpression;

						if (isSameIdentifier(visited, 0, calledObject)) {
							boolean valid= true;

							for (int i= 0; i < arguments.size(); i++) {
								ASTNode expression= ASTNodes.getUnparenthesedExpression(arguments.get(i));

								if (!(expression instanceof SimpleName) || !isSameIdentifier(visited, i + 1, (SimpleName) expression)) {
									valid= false;
									break;
								}
							}

							if (valid) {
								ITypeBinding klass= null;

								if (calledExpression.resolveTypeBinding() != null) {
									klass= calledExpression.resolveTypeBinding();
								} else if (methodInvocation.resolveMethodBinding() != null && methodInvocation.resolveMethodBinding().getDeclaringClass() != null) {
									klass= methodInvocation.resolveMethodBinding().getDeclaringClass();
								}

								if (klass != null) {
									String[] cumulativeParams= new String[arguments.size() + 1];
									cumulativeParams[0]= klass.getQualifiedName();

									for (int i= 0; i < arguments.size(); i++) {
										ITypeBinding resolveTypeBinding= arguments.get(i).resolveTypeBinding();

										if (resolveTypeBinding == null) {
											valid= false;
											break;
										}

										cumulativeParams[i + 1]= resolveTypeBinding.getQualifiedName();
									}

									if (valid) {
										for (IMethodBinding declaredMethodBinding : klass.getDeclaredMethods()) {
											if (Modifier.isStatic(declaredMethodBinding.getModifiers())
													&& ASTNodes.usesGivenSignature(declaredMethodBinding, klass.getQualifiedName(), methodInvocation.getName().getIdentifier(), cumulativeParams)) {
												valid= false;
												break;
											}
										}
									}

									if (valid) {
										actionType= ActionType.TYPE_REF;
										classBinding= klass;
									}
								}
							}
						}
					}
				}

				if (removeParamParentheses || actionType != ActionType.DO_NOTHING) {
					rewriteOperations.add(new ReplaceLambdaOperation(visited, removeParamParentheses, actionType, bodyExpression, classBinding));
					return false;
				}

				return true;
			}

			private boolean hasToRemoveParamParentheses(final LambdaExpression node) {
				return node.hasParentheses()
						&& node.parameters().size() == 1
						&& node.parameters().get(0) instanceof VariableDeclarationFragment;
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
					VariableDeclarationFragment variableDeclarationFragment= (VariableDeclarationFragment) param0;
					return variableDeclarationFragment.getName().getIdentifier().equals(argument.getIdentifier());
				}

				return false;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class ReplaceLambdaOperation extends CompilationUnitRewriteOperation {
		private final LambdaExpression visited;
		private final boolean removeParentheses;
		private final ActionType action;
		private final Expression bodyExpression;
		private final ITypeBinding classBinding;

		public ReplaceLambdaOperation(final LambdaExpression visited, final boolean removeParentheses, final ActionType action, final Expression bodyExpression, final ITypeBinding classBinding) {
			this.visited= visited;
			this.removeParentheses= removeParentheses;
			this.action= action;
			this.bodyExpression= bodyExpression;
			this.classBinding= classBinding;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description, cuRewrite);

			switch (action) {
				case REMOVE_RETURN:
					LambdaExpression copyOfLambdaExpression2= ast.newLambdaExpression();
					copyParameters(rewrite, visited, copyOfLambdaExpression2);

					if (removeParentheses) {
						copyOfLambdaExpression2.setParentheses(false);
					}

					copyOfLambdaExpression2.setBody(ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, bodyExpression)));
					ASTNodes.replaceButKeepComment(rewrite, visited, copyOfLambdaExpression2, group);
					break;

				case TYPE_REF:
					TypeMethodReference typeMethodRef= ast.newTypeMethodReference();
					MethodInvocation typeMethodInvocation= (MethodInvocation) bodyExpression;

					typeMethodRef.setType(copyType(cuRewrite, ast, typeMethodInvocation, classBinding));
					typeMethodRef.setName(ASTNodes.createMoveTarget(rewrite, typeMethodInvocation.getName()));
					ASTNodes.replaceButKeepComment(rewrite, visited, typeMethodRef, group);
					break;

				case METHOD_REF:
					ExpressionMethodReference methodRef= ast.newExpressionMethodReference();
					MethodInvocation methodInvocation= (MethodInvocation) bodyExpression;

					if (methodInvocation.getExpression() != null) {
						methodRef.setExpression(ASTNodes.createMoveTarget(rewrite, methodInvocation.getExpression()));
					} else {
						methodRef.setExpression(ast.newThisExpression());
					}

					methodRef.setName(ASTNodes.createMoveTarget(rewrite, methodInvocation.getName()));
					ASTNodes.replaceButKeepComment(rewrite, visited, methodRef, group);
					break;

				case SUPER_METHOD_REF:
					SuperMethodReference superMethodRef= ast.newSuperMethodReference();
					SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) bodyExpression;

					superMethodRef.setName(ASTNodes.createMoveTarget(rewrite, superMethodInvocation.getName()));
					ASTNodes.replaceButKeepComment(rewrite, visited, superMethodRef, group);
					break;

				case CLASS_INSTANCE_REF:
					CreationReference creationRef= ast.newCreationReference();
					ClassInstanceCreation classInstanceCreation= (ClassInstanceCreation) bodyExpression;

					creationRef.setType(copyType(cuRewrite, ast, classInstanceCreation, classInstanceCreation.resolveTypeBinding()));
					ASTNodes.replaceButKeepComment(rewrite, visited, creationRef, group);
					break;

				case DO_NOTHING:
				default:
					LambdaExpression copyOfLambdaExpression= ast.newLambdaExpression();
					copyParameters(rewrite, visited, copyOfLambdaExpression);

					if (removeParentheses) {
						copyOfLambdaExpression.setParentheses(false);
					}

					copyOfLambdaExpression.setBody(rewrite.createMoveTarget(visited.getBody()));
					ASTNodes.replaceButKeepComment(rewrite, visited, copyOfLambdaExpression, group);
					break;

			}
		}

		private void copyParameters(final ASTRewrite rewrite, final LambdaExpression oldLambdaExpression, final LambdaExpression copyOfLambdaExpression) {
			for (Object oldParameter : oldLambdaExpression.parameters()) {
				ASTNode copyOfParameter= ASTNodes.createMoveTarget(rewrite, (ASTNode) oldParameter);
				copyOfLambdaExpression.parameters().add(copyOfParameter);
			}

			copyOfLambdaExpression.setParentheses(oldLambdaExpression.hasParentheses());
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
}