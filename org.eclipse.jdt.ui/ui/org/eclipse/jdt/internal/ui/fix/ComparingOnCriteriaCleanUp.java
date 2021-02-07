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
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.OrderedInfixExpression;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.ControlWorkflowMatcher;
import org.eclipse.jdt.internal.corext.util.ControlWorkflowMatcherRunnable;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.NodeMatcher;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that replaces a plain comparator instance by a lambda expression passed to a <code>Comparator.comparing()</code> method:
 * <ul>
 * <li>The <code>Comparator</code> type must be inferred by the destination of the comparator,</li>
 * <li>The algorithm of the comparator must be standard and based on one field or method,</li>
 * <li>The cleanup can handle the null values.</li>
 * </ul>
 */
public class ComparingOnCriteriaCleanUp extends AbstractMultiFix {
	private static final class ObjectNotNullMatcher extends NodeMatcher<Expression> {
		private final SimpleName name;

		private ObjectNotNullMatcher(final SimpleName name) {
			this.name= name;
		}

		@Override
		public Boolean isMatching(final Expression node) {
			InfixExpression condition= ASTNodes.as(node, InfixExpression.class);

			if (condition != null
					&& ASTNodes.hasOperator(condition, InfixExpression.Operator.EQUALS, InfixExpression.Operator.NOT_EQUALS)) {
				OrderedInfixExpression<SimpleName, NullLiteral> orderedInfix= ASTNodes.orderedInfix(condition, SimpleName.class, NullLiteral.class);

				if (orderedInfix != null
						&& ASTNodes.isSameVariable(orderedInfix.getFirstOperand(), name)
						&& ASTNodes.isPassive(orderedInfix.getFirstOperand())) {
					return ASTNodes.hasOperator(condition, InfixExpression.Operator.NOT_EQUALS);
				}
			}

			return null;
		}
	}

	public ComparingOnCriteriaCleanUp() {
		this(Collections.emptyMap());
	}

	public ComparingOnCriteriaCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.COMPARING_ON_CRITERIA);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.COMPARING_ON_CRITERIA)) {
			return new String[] { MultiFixMessages.ComparingOnCriteriaCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.COMPARING_ON_CRITERIA)) {
			return "Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString));\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "Comparator<Date> comparator = new Comparator<Date>() {\n" //$NON-NLS-1$
				+ "    @Override\n" //$NON-NLS-1$
				+ "    public int compare(Date o1, Date o2) {\n" //$NON-NLS-1$
				+ "        if (o2 != null) {\n" //$NON-NLS-1$
				+ "            if (o1 != null) {\n" //$NON-NLS-1$
				+ "                return o1.toString().compareTo(o2.toString());\n" //$NON-NLS-1$
				+ "            }\n" //$NON-NLS-1$
				+ "            return -1;\n" //$NON-NLS-1$
				+ "        } else if (o1 != null) {\n" //$NON-NLS-1$
				+ "            return 1;\n" //$NON-NLS-1$
				+ "        } else {\n" //$NON-NLS-1$
				+ "            return 0;\n" //$NON-NLS-1$
				+ "        }\n" //$NON-NLS-1$
				+ "    }\n" //$NON-NLS-1$
				+ "};\n"; //$NON-NLS-1$
	}

	private static boolean equalNotNull(final Object obj1, final Object obj2) {
		return obj1 != null && obj1.equals(obj2);
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.COMPARING_ON_CRITERIA) || !JavaModelUtil.is1d8OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final LambdaExpression visited) {
				ITypeBinding targetType= ASTNodes.getTargetType(visited);

				if (ASTNodes.hasType(targetType, Comparator.class.getCanonicalName())
						&& targetType.getTypeArguments() != null
						&& targetType.getTypeArguments().length == 1
						&& visited.parameters() != null
						&& visited.parameters().size() == 2) {
					VariableDeclaration object1= (VariableDeclaration) visited.parameters().get(0);
					VariableDeclaration object2= (VariableDeclaration) visited.parameters().get(1);

					if (visited.getBody() instanceof Statement) {
						return maybeRefactorBody(visited, targetType.getTypeArguments()[0], object1, object2, ASTNodes.asList((Statement) visited.getBody()));
					}

				    if (visited.getBody() instanceof Expression) {
						SimpleName name1= object1.getName();
						SimpleName name2= object2.getName();

						return maybeRefactorExpression(visited, targetType.getTypeArguments()[0], name1, name2, (Expression) visited.getBody());
					}
				}

				return true;
			}

			@Override
			public boolean visit(final ClassInstanceCreation visited) {
				AnonymousClassDeclaration anonymousClassDecl= visited.getAnonymousClassDeclaration();
				Type type= visited.getType();

				if (type != null
						&& type.resolveBinding() != null
						&& type.resolveBinding().getTypeArguments() != null
						&& type.resolveBinding().getTypeArguments().length == 1
						&& ASTNodes.hasType(type.resolveBinding(), Comparator.class.getCanonicalName())
						&& visited.arguments().isEmpty()
						&& anonymousClassDecl != null
						&& anonymousClassDecl.bodyDeclarations() != null
						&& anonymousClassDecl.bodyDeclarations().size() == 1) {
					List<BodyDeclaration> bodies= anonymousClassDecl.bodyDeclarations();
					ITypeBinding typeArgument= type.resolveBinding().getTypeArguments()[0];

					if (bodies != null
							&& bodies.size() == 1
							&& typeArgument != null) {
						BodyDeclaration body= bodies.get(0);

						if (body instanceof MethodDeclaration) {
							return maybeRefactorMethod(visited, typeArgument, (MethodDeclaration) body);
						}
					}
				}

				return true;
			}

			private boolean maybeRefactorMethod(final ClassInstanceCreation visited, final ITypeBinding typeArgument,
					final MethodDeclaration methodDecl) {
				Block methodBody= methodDecl.getBody();

				if (ASTNodes.usesGivenSignature(methodDecl, Comparator.class.getCanonicalName(), "compare", typeArgument.getQualifiedName(), //$NON-NLS-1$
						typeArgument.getQualifiedName())) {
					VariableDeclaration object1= (VariableDeclaration) methodDecl.parameters().get(0);
					VariableDeclaration object2= (VariableDeclaration) methodDecl.parameters().get(1);

					List<Statement> statements= methodBody.statements();

					return maybeRefactorBody(visited, typeArgument, object1, object2, statements);
				}

				return true;
			}

			private boolean maybeRefactorBody(final Expression visited, final ITypeBinding typeArgument,
					final VariableDeclaration object1, final VariableDeclaration object2, final List<Statement> statements) {
				SimpleName name1= object1.getName();
				SimpleName name2= object2.getName();

				if (!maybeRefactorCompareToMethod(visited, typeArgument, statements, name1, name2)) {
					return false;
				}

				AtomicReference<Expression> criteria= new AtomicReference<>();
				AtomicBoolean isForward= new AtomicBoolean(true);

				NodeMatcher<Expression> compareToMatcher= new NodeMatcher<Expression>() {
					@Override
					public Boolean isMatching(final Expression node) {
						if (isReturnedExpressionToRefactor(node, criteria, isForward, name1, name2)) {
							return Boolean.TRUE;
						}

						return null;
					}
				};

				NodeMatcher<Expression> zeroMatcher= new NodeMatcher<Expression>() {
					@Override
					public Boolean isMatching(final Expression node) {
						if (Long.valueOf(0L).equals(ASTNodes.getIntegerLiteral(node))) {
							return Boolean.TRUE;
						}

						return null;
					}
				};

				NodeMatcher<Expression> positiveMatcher= new NodeMatcher<Expression>() {
					@Override
					public Boolean isMatching(final Expression node) {
						Long value= ASTNodes.getIntegerLiteral(node);

						if (value != null && value > 0L) {
							return Boolean.TRUE;
						}

						return null;
					}
				};

				NodeMatcher<Expression> negativeMatcher= new NodeMatcher<Expression>() {
					@Override
					public Boolean isMatching(final Expression node) {
						Long value= ASTNodes.getIntegerLiteral(node);

						if (value != null && value < 0L) {
							return Boolean.TRUE;
						}

						return null;
					}
				};

				ControlWorkflowMatcherRunnable runnableMatcher= ControlWorkflowMatcher.createControlWorkflowMatcher().addWorkflow(new ObjectNotNullMatcher(name1)).condition(new ObjectNotNullMatcher(name2)).returnedValue(compareToMatcher)
						.addWorkflow(new ObjectNotNullMatcher(name1).negate()).condition(new ObjectNotNullMatcher(name2).negate()).returnedValue(zeroMatcher)
						.addWorkflow(new ObjectNotNullMatcher(name1).negate()).condition(new ObjectNotNullMatcher(name2)).returnedValue(negativeMatcher)
						.addWorkflow(new ObjectNotNullMatcher(name1)).condition(new ObjectNotNullMatcher(name2).negate()).returnedValue(positiveMatcher);

				if (runnableMatcher.isMatching(statements)) {
					rewriteOperations.add(new ComparingOnCriteriaOperation(visited, typeArgument, name1, criteria, isForward, Boolean.TRUE));
					return false;
				}

				runnableMatcher= ControlWorkflowMatcher.createControlWorkflowMatcher().addWorkflow(new ObjectNotNullMatcher(name1)).condition(new ObjectNotNullMatcher(name2)).returnedValue(compareToMatcher)
						.addWorkflow(new ObjectNotNullMatcher(name1).negate()).condition(new ObjectNotNullMatcher(name2).negate()).returnedValue(zeroMatcher)
						.addWorkflow(new ObjectNotNullMatcher(name1)).condition(new ObjectNotNullMatcher(name2).negate()).returnedValue(negativeMatcher)
						.addWorkflow(new ObjectNotNullMatcher(name1).negate()).condition(new ObjectNotNullMatcher(name2)).returnedValue(positiveMatcher);

				if (runnableMatcher.isMatching(statements)) {
					rewriteOperations.add(new ComparingOnCriteriaOperation(visited, typeArgument, name1, criteria, isForward, Boolean.FALSE));
					return false;
				}

				return true;
			}

			private boolean maybeRefactorCompareToMethod(final Expression visited, final ITypeBinding typeArgument,
					final List<Statement> statements, final SimpleName name1,
					final SimpleName name2) {
				if (statements != null && statements.size() == 1) {
					ReturnStatement returnStatement= ASTNodes.as(statements.get(0), ReturnStatement.class);

					if (returnStatement != null) {
						return maybeRefactorExpression(visited, typeArgument, name1, name2, returnStatement.getExpression());
					}
				}

				return true;
			}

			private boolean maybeRefactorExpression(final Expression visited, final ITypeBinding typeArgument,
					final SimpleName name1, final SimpleName name2,
					final Expression expression) {
				AtomicReference<Expression> criteria= new AtomicReference<>();
				AtomicBoolean isForward= new AtomicBoolean(true);

				if (isReturnedExpressionToRefactor(expression, criteria, isForward, name1, name2)) {
					rewriteOperations.add(new ComparingOnCriteriaOperation(visited, typeArgument, name1, criteria, isForward, null));
					return false;
				}

				return true;
			}

			private boolean isComparable(ITypeBinding classBinding) {
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

			private boolean isReturnedExpressionToRefactor(final Expression returnExpression, final AtomicReference<Expression> criteria,
					final AtomicBoolean isForward, final SimpleName name1,
					final SimpleName name2) {
				PrefixExpression negativeExpression= ASTNodes.as(returnExpression, PrefixExpression.class);

				if (negativeExpression != null && ASTNodes.hasOperator(negativeExpression, PrefixExpression.Operator.MINUS)) {
					isForward.lazySet(!isForward.get());
					return isReturnedExpressionToRefactor(negativeExpression.getOperand(), criteria, isForward, name1, name2);
				}

				MethodInvocation compareToMethod= ASTNodes.as(returnExpression, MethodInvocation.class);

				if (compareToMethod != null && compareToMethod.getExpression() != null) {
					ITypeBinding comparisonType= compareToMethod.getExpression().resolveTypeBinding();

					if (comparisonType != null && isComparable(comparisonType)) {
						if (compareToMethod.getExpression() != null
								&& ASTNodes.usesGivenSignature(compareToMethod, comparisonType.getQualifiedName(), "compareTo", comparisonType.getQualifiedName())) { //$NON-NLS-1$
							return isRefactorComparisonToRefactor(criteria, isForward, name1, name2, compareToMethod.getExpression(), (Expression) compareToMethod.arguments().get(0));
						}

						String primitiveType= Bindings.getUnboxedTypeName(comparisonType.getQualifiedName());

						if (primitiveType != null
								&& ASTNodes.usesGivenSignature(compareToMethod, comparisonType.getQualifiedName(), "compare", primitiveType, primitiveType)) { //$NON-NLS-1$
							return isRefactorComparisonToRefactor(criteria, isForward, name1, name2, (Expression) compareToMethod.arguments().get(0), (Expression) compareToMethod.arguments().get(1));
						}
					}
				}

				return false;
			}

			private boolean isRefactorComparisonToRefactor(final AtomicReference<Expression> criteria,
					final AtomicBoolean isForward, final SimpleName name1, final SimpleName name2, final Expression expr1,
					final Expression expr2) {
				MethodInvocation method1= ASTNodes.as(expr1, MethodInvocation.class);
				MethodInvocation method2= ASTNodes.as(expr2, MethodInvocation.class);

				QualifiedName field1= ASTNodes.as(expr1, QualifiedName.class);
				QualifiedName field2= ASTNodes.as(expr2, QualifiedName.class);

				if (method1 != null
						&& method1.arguments().isEmpty()
						&& method2 != null
						&& method2.arguments().isEmpty()) {
					String methodName1= method1.getName().getIdentifier();
					String methodName2= method2.getName().getIdentifier();

					SimpleName objectExpr1= ASTNodes.as(method1.getExpression(), SimpleName.class);
					SimpleName objectExpr2= ASTNodes.as(method2.getExpression(), SimpleName.class);

					if (equalNotNull(methodName1, methodName2)
							&& objectExpr1 != null
							&& objectExpr2 != null) {
						if (ASTNodes.isSameVariable(objectExpr1, name1)
								&& ASTNodes.isSameVariable(objectExpr2, name2)) {
							criteria.set(method1);
							return true;
						}

						if (ASTNodes.isSameVariable(objectExpr1, name2)
								&& ASTNodes.isSameVariable(objectExpr2, name1)) {
							criteria.set(method1);
							isForward.lazySet(!isForward.get());
							return true;
						}
					}
				} else if (field1 != null && field2 != null) {
					SimpleName fieldName1= field1.getName();
					SimpleName fieldName2= field2.getName();

					SimpleName objectExpr1= ASTNodes.as(field1.getQualifier(), SimpleName.class);
					SimpleName objectExpr2= ASTNodes.as(field2.getQualifier(), SimpleName.class);

					if (ASTNodes.isSameVariable(fieldName1, fieldName2)
							&& objectExpr1 != null
							&& objectExpr2 != null) {
						if (ASTNodes.isSameVariable(objectExpr1, name1)
								&& ASTNodes.isSameVariable(objectExpr2, name2)) {
							criteria.set(field1);
							return true;
						}

						if (ASTNodes.isSameVariable(objectExpr1, name2)
								&& ASTNodes.isSameVariable(objectExpr2, name1)) {
							criteria.set(field1);
							isForward.lazySet(!isForward.get());
							return true;
						}
					}
				}

				return false;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.ComparingOnCriteriaCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class ComparingOnCriteriaOperation extends CompilationUnitRewriteOperation {
		private final Expression visited;
		private final ITypeBinding typeArgument;
		private final SimpleName name1;
		private final AtomicReference<Expression> criteria;
		private final AtomicBoolean isForward;
		private final Boolean isNullFirst;

		public ComparingOnCriteriaOperation(final Expression visited, final ITypeBinding typeArgument,
				final SimpleName name1, final AtomicReference<Expression> criteria, final AtomicBoolean isForward, final Boolean isNullFirst) {
			this.visited= visited;
			this.typeArgument= typeArgument;
			this.name1= name1;
			this.criteria= criteria;
			this.isForward= isForward;
			this.isNullFirst= isNullFirst;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ComparingOnCriteriaCleanUp_description, cuRewrite);

			String comparatorNameText= importRewrite.addImport(Comparator.class.getCanonicalName());

			Expression lambda;
			if (criteria.get() instanceof MethodInvocation) {
				lambda= buildMethod(typeArgument, (MethodInvocation) criteria.get(), rewrite, ast, importRewrite);
			} else {
				lambda= buildField(visited, typeArgument, isForward.get(), isNullFirst, (QualifiedName) criteria.get(), name1, rewrite, ast, importRewrite);
			}

			MethodInvocation comparingMethod= ast.newMethodInvocation();
			comparingMethod.setExpression(ASTNodeFactory.newName(ast, comparatorNameText));
			comparingMethod.setName(ast.newSimpleName("comparing")); //$NON-NLS-1$
			comparingMethod.arguments().add(lambda);

			if (!isForward.get()) {
				MethodInvocation newMethodInvocation= ast.newMethodInvocation();
				newMethodInvocation.setExpression(comparingMethod);
				newMethodInvocation.setName(ast.newSimpleName("reversed")); //$NON-NLS-1$
				comparingMethod= newMethodInvocation;
			}

			if (isNullFirst != null) {
				MethodInvocation newMethodInvocation= ast.newMethodInvocation();
				newMethodInvocation.setExpression(ASTNodeFactory.newName(ast, comparatorNameText));
				newMethodInvocation.setName(ast.newSimpleName(isNullFirst ? "nullsFirst" : "nullsLast")); //$NON-NLS-1$ //$NON-NLS-2$
				newMethodInvocation.arguments().add(comparingMethod);
				comparingMethod= newMethodInvocation;
			}

			ASTNodes.replaceButKeepComment(rewrite, visited, comparingMethod, group);
		}

		private TypeMethodReference buildMethod(final ITypeBinding type, final MethodInvocation method, final ASTRewrite rewrite, final AST ast, final ImportRewrite importRewrite) {
			String comparedClassNameText= importRewrite.addImport((type.getBound() != null && !type.isUpperbound()) ? type.getBound() : type.getErasure());

			TypeMethodReference typeMethodRef= ast.newTypeMethodReference();
			typeMethodRef.setType(ast.newSimpleType(ASTNodeFactory.newName(ast, comparedClassNameText)));
			typeMethodRef.setName(ASTNodes.createMoveTarget(rewrite, method.getName()));
			return typeMethodRef;
		}

		private LambdaExpression buildField(final Expression node, final ITypeBinding type, final boolean straightOrder,
				final Boolean isNullValuesFirst, final QualifiedName field, final SimpleName name, final ASTRewrite rewrite, final AST ast, final ImportRewrite importRewrite) {
			LambdaExpression lambdaExpression= ast.newLambdaExpression();
			ITypeBinding destinationType= ASTNodes.getTargetType(node);

			boolean isTypeKnown= destinationType != null
					&& ASTNodes.hasType(destinationType, Comparator.class.getCanonicalName())
					&& destinationType.getTypeArguments() != null
					&& destinationType.getTypeArguments().length == 1
					&& equalNotNull(destinationType.getTypeArguments()[0], type);

			if (isTypeKnown && straightOrder && isNullValuesFirst == null) {
				VariableDeclarationFragment newVariableDeclarationFragment= ast.newVariableDeclarationFragment();
				newVariableDeclarationFragment.setName((SimpleName) rewrite.createCopyTarget(name));
				lambdaExpression.parameters().add(newVariableDeclarationFragment);
			} else {
				String comparedClassNameText= importRewrite.addImport(type);

				SingleVariableDeclaration newSingleVariableDeclaration= ast.newSingleVariableDeclaration();
				newSingleVariableDeclaration.setType(ast.newSimpleType(ASTNodeFactory.newName(ast, comparedClassNameText)));
				newSingleVariableDeclaration.setName((SimpleName) rewrite.createCopyTarget(name));
				lambdaExpression.parameters().add(newSingleVariableDeclaration);
			}

			FieldAccess newFieldAccess= ast.newFieldAccess();
			newFieldAccess.setExpression((SimpleName) rewrite.createCopyTarget(name));
			newFieldAccess.setName(ASTNodes.createMoveTarget(rewrite, field.getName()));
			lambdaExpression.setBody(newFieldAccess);
			lambdaExpression.setParentheses(false);
			return lambdaExpression;
		}
	}
}
