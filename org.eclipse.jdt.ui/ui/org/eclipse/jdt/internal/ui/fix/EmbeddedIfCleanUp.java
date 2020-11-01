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
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that raises embedded if into parent if.
 */
public class EmbeddedIfCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public EmbeddedIfCleanUp() {
		this(Collections.emptyMap());
	}

	public EmbeddedIfCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.RAISE_EMBEDDED_IF);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.RAISE_EMBEDDED_IF)) {
			return new String[] { MultiFixMessages.EmbeddedIfCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.RAISE_EMBEDDED_IF)) {
			return "" //$NON-NLS-1$
					+ "if (isActive && isValid) {\n" //$NON-NLS-1$
					+ "  int i = 0;\n" //$NON-NLS-1$
					+ "}\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (isActive) {\n" //$NON-NLS-1$
				+ "  if (isValid) {\n" //$NON-NLS-1$
				+ "    int i = 0;\n" //$NON-NLS-1$
				+ "  }\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.RAISE_EMBEDDED_IF)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement visited) {
				if (visited.getElseStatement() == null) {
					IfStatement innerIf= ASTNodes.as(visited.getThenStatement(), IfStatement.class);

					if (innerIf != null
							&& innerIf.getElseStatement() == null
							&& ASTNodes.getNbOperands(visited.getExpression()) + ASTNodes.getNbOperands(innerIf.getExpression()) < ASTNodes.EXCESSIVE_OPERAND_NUMBER) {
						// The parsing crashes when there are two embedded lone ifs with an end of line comment at the right of the statement
						// So we disable the rule on double lone if
						if (!(visited.getThenStatement() instanceof Block)
								&& !(innerIf.getThenStatement() instanceof Block)) {
							return true;
						}

						rewriteOperations.add(new EmbeddedIfOperation(visited, innerIf));
						return false;
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.EmbeddedIfCleanup_description, unit,
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

	private static class EmbeddedIfOperation extends CompilationUnitRewriteOperation {
		private final IfStatement visited;
		private final IfStatement innerIf;

		public EmbeddedIfOperation(final IfStatement visited, final IfStatement innerIf) {
			this.visited= visited;
			this.innerIf= innerIf;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.EmbeddedIfCleanup_description, cuRewrite);

			InfixExpression infixExpression= ast.newInfixExpression();
			infixExpression.setLeftOperand(ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, visited.getExpression())));
			infixExpression.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
			infixExpression.setRightOperand(ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, innerIf.getExpression())));
			ASTNodes.replaceButKeepComment(rewrite, innerIf.getExpression(), infixExpression, group);
			ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodes.createMoveTarget(rewrite, innerIf), group);
		}
	}
}
