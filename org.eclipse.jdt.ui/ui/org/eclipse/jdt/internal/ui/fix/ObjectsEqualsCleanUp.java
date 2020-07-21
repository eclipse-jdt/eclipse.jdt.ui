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
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.OrderedInfixExpression;
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
 * A fix that reduces the code of the equals method implementation by using Objects.equals().
 */
public class ObjectsEqualsCleanUp extends AbstractMultiFix implements ICleanUpFix {
	private static final String EQUALS_METHOD= "equals"; //$NON-NLS-1$

	public ObjectsEqualsCleanUp() {
		this(Collections.emptyMap());
	}

	public ObjectsEqualsCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.USE_OBJECTS_EQUALS);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_OBJECTS_EQUALS)) {
			return new String[] { MultiFixMessages.ObjectsEqualsCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.USE_OBJECTS_EQUALS)) {
			return "" //$NON-NLS-1$
					+ "if (!Objects.equals(aText, other.aText)) {\n" //$NON-NLS-1$
					+ "	return false;\n" //$NON-NLS-1$
					+ "}\n\n\n\n\n"; //$NON-NLS-1$
		} else {
			return "" //$NON-NLS-1$
					+ "if (aText == null) {\n" //$NON-NLS-1$
					+ "  if (other.aText != null) {\n" //$NON-NLS-1$
					+ "    return false;\n" //$NON-NLS-1$
					+ "  }\n" //$NON-NLS-1$
					+ "} else if (!aText.equals(other.aText)) {\n" //$NON-NLS-1$
					+ "	return false;\n" //$NON-NLS-1$
					+ "}\n"; //$NON-NLS-1$
		}
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.USE_OBJECTS_EQUALS) || !JavaModelUtil.is17OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {

			@Override
			public boolean visit(final IfStatement node) {
				if (node.getElseStatement() != null) {
					InfixExpression condition= ASTNodes.as(node.getExpression(), InfixExpression.class);
					List<Statement> thenStatements= ASTNodes.asList(node.getThenStatement());
					List<Statement> elseStatements= ASTNodes.asList(node.getElseStatement());

					if (condition != null && !condition.hasExtendedOperands()
							&& ASTNodes.hasOperator(condition, InfixExpression.Operator.EQUALS, InfixExpression.Operator.NOT_EQUALS)
							&& thenStatements != null && thenStatements.size() == 1 && elseStatements != null && elseStatements.size() == 1) {
						OrderedInfixExpression<Expression, NullLiteral> nullityTypedCondition= ASTNodes.orderedInfix(condition, Expression.class, NullLiteral.class);

						if (nullityTypedCondition != null && ASTNodes.isPassive(nullityTypedCondition.getFirstOperand())) {
							return maybeReplaceCode(node, condition, thenStatements, elseStatements, nullityTypedCondition.getFirstOperand());
						}
					}
				}

				return true;
			}

			private boolean maybeReplaceCode(final IfStatement node, final InfixExpression condition,
					final List<Statement> thenStatements, final List<Statement> elseStatements, final Expression firstField) {
				IfStatement checkNullityStatement;
				IfStatement checkEqualsStatement;

				if (ASTNodes.hasOperator(condition, InfixExpression.Operator.EQUALS)) {
					checkNullityStatement= ASTNodes.as(thenStatements.get(0), IfStatement.class);
					checkEqualsStatement= ASTNodes.as(elseStatements.get(0), IfStatement.class);
				} else {
					checkEqualsStatement= ASTNodes.as(thenStatements.get(0), IfStatement.class);
					checkNullityStatement= ASTNodes.as(elseStatements.get(0), IfStatement.class);
				}

				if (checkNullityStatement != null && checkNullityStatement.getElseStatement() == null && checkEqualsStatement != null
						&& checkEqualsStatement.getElseStatement() == null) {
					InfixExpression nullityCondition= ASTNodes.as(checkNullityStatement.getExpression(), InfixExpression.class);
					List<Statement> nullityStatements= ASTNodes.asList(checkNullityStatement.getThenStatement());

					PrefixExpression equalsCondition= ASTNodes.as(checkEqualsStatement.getExpression(), PrefixExpression.class);
					List<Statement> equalsStatements= ASTNodes.asList(checkEqualsStatement.getThenStatement());

					if (nullityCondition != null && !nullityCondition.hasExtendedOperands()
							&& ASTNodes.hasOperator(nullityCondition, InfixExpression.Operator.NOT_EQUALS) && nullityStatements != null
							&& nullityStatements.size() == 1 && equalsCondition != null
							&& ASTNodes.hasOperator(equalsCondition, PrefixExpression.Operator.NOT) && equalsStatements != null
							&& equalsStatements.size() == 1) {
						return maybeReplaceEquals(node, firstField, nullityCondition, nullityStatements, equalsCondition,
								equalsStatements);
					}
				}

				return true;
			}

			private boolean maybeReplaceEquals(final IfStatement node, final Expression firstField,
					final InfixExpression nullityCondition, final List<Statement> nullityStatements,
					final PrefixExpression equalsCondition, final List<Statement> equalsStatements) {
				OrderedInfixExpression<Expression, NullLiteral> nullityTypedCondition= ASTNodes.orderedInfix(nullityCondition, Expression.class, NullLiteral.class);
				ReturnStatement returnStatement1= ASTNodes.as(nullityStatements.get(0), ReturnStatement.class);
				ReturnStatement returnStatement2= ASTNodes.as(equalsStatements.get(0), ReturnStatement.class);
				MethodInvocation equalsMethod= ASTNodes.as(equalsCondition.getOperand(), MethodInvocation.class);

				if (nullityTypedCondition != null && returnStatement1 != null && returnStatement2 != null && equalsMethod != null
						&& equalsMethod.getExpression() != null && EQUALS_METHOD.equals(equalsMethod.getName().getIdentifier())
						&& equalsMethod.arguments() != null && equalsMethod.arguments().size() == 1) {
					Expression secondField= nullityTypedCondition.getFirstOperand();

					if (secondField != null
							&& (match(firstField, secondField, equalsMethod.getExpression(),
									(ASTNode) equalsMethod.arguments().get(0))
									|| match(secondField, firstField, equalsMethod.getExpression(),
											(ASTNode) equalsMethod.arguments().get(0)))) {
						BooleanLiteral returnFalse1= ASTNodes.as(returnStatement1.getExpression(), BooleanLiteral.class);
						BooleanLiteral returnFalse2= ASTNodes.as(returnStatement2.getExpression(), BooleanLiteral.class);

						if (returnFalse1 != null && !returnFalse1.booleanValue() && returnFalse2 != null
								&& !returnFalse2.booleanValue()) {
							rewriteOperations.add(new ObjectsEqualsOperation(node, firstField, secondField, returnStatement1));
							return false;
						}
					}
				}

				return true;
			}

			private boolean match(final Expression firstField, final Expression secondField, final Expression thisObject,
					final ASTNode otherObject) {
				ASTMatcher astMatcher= new ASTMatcher();
				return astMatcher.safeSubtreeMatch(thisObject, firstField) && astMatcher.safeSubtreeMatch(otherObject, secondField);
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.ObjectsEqualsCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		return null;
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class ObjectsEqualsOperation extends CompilationUnitRewriteOperation {
		private final IfStatement node;
		private final Expression firstField;
		private final Expression secondField;
		private final ReturnStatement returnStatement;

		public ObjectsEqualsOperation(final IfStatement node, final Expression firstField,
				final Expression secondField,
				final ReturnStatement returnStatement) {
			this.node= node;
			this.firstField= firstField;
			this.secondField= secondField;
			this.returnStatement= returnStatement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ObjectsEqualsCleanup_description, cuRewrite);
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();

			String objectNameText= importRewrite.addImport(Objects.class.getCanonicalName());

			MethodInvocation newEqualsMethod= ast.newMethodInvocation();
			newEqualsMethod.setExpression(newTypeName(ast, objectNameText));
			newEqualsMethod.setName(ast.newSimpleName(EQUALS_METHOD));
			newEqualsMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, firstField));
			newEqualsMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, secondField));

			PrefixExpression notExpression= ast.newPrefixExpression();
			notExpression.setOperator(PrefixExpression.Operator.NOT);
			notExpression.setOperand(newEqualsMethod);

			ReturnStatement copyOfReturnStatement= ASTNodes.createMoveTarget(rewrite, returnStatement);

			rewrite.replace(node.getExpression(), notExpression, group);

			if (node.getThenStatement() instanceof Block) {
				rewrite.replace((ASTNode) ((Block) node.getThenStatement()).statements().get(0), copyOfReturnStatement, group);
			} else {
				rewrite.replace(node.getThenStatement(), copyOfReturnStatement, group);
			}

			if (node.getElseStatement() != null) {
				rewrite.remove(node.getElseStatement(), group);
			}
		}

		private Name newTypeName(final AST ast, final String objectNameText) {
			Name qualifiedName= null;

			for (String packageName : objectNameText.split("\\.")) { //$NON-NLS-1$
				if (qualifiedName == null) {
					qualifiedName= ast.newSimpleName(packageName);
				} else {
					qualifiedName= ast.newQualifiedName(qualifiedName, ast.newSimpleName(packageName));
				}
			}

			return qualifiedName;
		}
	}
}
