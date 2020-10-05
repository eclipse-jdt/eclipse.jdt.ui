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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTSemanticMatcher;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that removes a condition on an else that is negative to the condition of the previous if.
 * Beware! It may change the behavior if the code is transpiled in JavaScript for NaN values:
 * <ul>
 * <li>The condition must be passive.</li>
 * <li>The removed code should not throw an expected exception.</li>
 * </ul>
 */
public class RedundantIfConditionCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public RedundantIfConditionCleanUp() {
		this(Collections.emptyMap());
	}

	public RedundantIfConditionCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REDUNDANT_IF_CONDITION);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REDUNDANT_IF_CONDITION)) {
			return new String[] { MultiFixMessages.RedundantIfConditionCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("if (isValid) {\n"); //$NON-NLS-1$
		bld.append("  return 0;\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.REDUNDANT_IF_CONDITION)) {
			bld.append("} else {\n"); //$NON-NLS-1$
		} else {
			bld.append("} else if (!isValid) {\n"); //$NON-NLS-1$
		}

		bld.append("  return -1;\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$
		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REDUNDANT_IF_CONDITION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement node) {
				IfStatement secondIf= ASTNodes.as(node.getElseStatement(), IfStatement.class);

				if (secondIf != null && (secondIf.getElseStatement() == null || !ASTNodes.isExceptionExpected(node))
						&& (!ASTNodes.fallsThrough(node.getThenStatement()) || !ASTNodes.fallsThrough(secondIf.getThenStatement()))
						&& ASTNodes.isPassive(node.getExpression()) && ASTNodes.isPassive(secondIf.getExpression())
						&& ASTSemanticMatcher.INSTANCE.matchNegative(node.getExpression(), secondIf.getExpression())) {
					rewriteOperations.add(new RedundantIfConditionOperation(secondIf));

					return false;
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.RedundantIfConditionCleanup_description, unit,
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

	private static class RedundantIfConditionOperation extends CompilationUnitRewriteOperation {
		private final IfStatement secondIf;

		public RedundantIfConditionOperation(final IfStatement secondIf) {
			this.secondIf= secondIf;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.RedundantIfConditionCleanup_description, cuRewrite);

			ASTNodes.replaceButKeepComment(rewrite, secondIf, ASTNodes.createMoveTarget(rewrite, secondIf.getThenStatement()), group);
		}
	}
}
