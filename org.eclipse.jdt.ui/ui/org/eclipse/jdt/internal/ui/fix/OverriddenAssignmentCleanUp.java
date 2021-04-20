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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that removes passive assignment when the variable is reassigned before being read.
 */
public class OverriddenAssignmentCleanUp extends AbstractMultiFix {
	public OverriddenAssignmentCleanUp() {
		this(Collections.emptyMap());
	}

	public OverriddenAssignmentCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			return new String[] { MultiFixMessages.OverriddenAssignmentCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			bld.append("long time;\n"); //$NON-NLS-1$
		} else {
			bld.append("long time = 0;\n"); //$NON-NLS-1$
		}

		bld.append("time = System.currentTimeMillis();\n"); //$NON-NLS-1$

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.OVERRIDDEN_ASSIGNMENT)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final VariableDeclarationStatement node) {
				VariableDeclarationFragment fragment= ASTNodes.getUniqueFragment(node);

				if (fragment != null
						&& fragment.getInitializer() != null
						&& ASTNodes.isPassiveWithoutFallingThrough(fragment.getInitializer())) {
					SimpleName varName= fragment.getName();
					IVariableBinding variable= fragment.resolveBinding();
					Statement stmtToInspect= ASTNodes.getNextSibling(node);
					boolean isOverridden= false;

					while (stmtToInspect != null) {
						if (!new VarDefinitionsUsesVisitor(variable, stmtToInspect, true).getReads().isEmpty()) {
							return true;
						}

						Assignment assignment= ASTNodes.asExpression(stmtToInspect, Assignment.class);

						if (assignment != null && ASTNodes.isSameVariable(varName, assignment.getLeftHandSide())) {
							if (!ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
								return true;
							}

							isOverridden= true;
							break;
						}

						stmtToInspect= ASTNodes.getNextSibling(stmtToInspect);
					}

					if (isOverridden) {
						rewriteOperations.add(new OverriddenAssignmentOperation(fragment.getInitializer()));
						return false;
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.OverriddenAssignmentCleanUp_description, unit,
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

	private static class OverriddenAssignmentOperation extends CompilationUnitRewriteOperation {
		private final Expression nodeToReplace;

		public OverriddenAssignmentOperation(final Expression expression) {
			this.nodeToReplace= expression;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.OverriddenAssignmentCleanUp_description, cuRewrite);

			rewrite.remove(nodeToReplace, group);
		}
	}
}
