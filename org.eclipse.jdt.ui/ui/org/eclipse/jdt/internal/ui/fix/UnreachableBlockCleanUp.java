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
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

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
 * A fix that detects two <code>if</code> conditions that are identical and removes the second one:
 * <ul>
 * <li>The conditions should be passive,</li>
 * <li>No exceptions should be awaited,</li>
 * <li>It should not create unreachable code below the <code>if</code> statement which would create a compile error, that is to say the case where
 * only the removed block doesn't fall through,
 * all the other cases fall through,
 * there is an <code>else</code> clause (not only <code>if</code>/<code>else</code> clauses)
 * and a statement after the control workflow.</li>
 * </ul>
 */
public class UnreachableBlockCleanUp extends AbstractMultiFix {
	public UnreachableBlockCleanUp() {
		this(Collections.emptyMap());
	}

	public UnreachableBlockCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.UNREACHABLE_BLOCK);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.UNREACHABLE_BLOCK)) {
			return new String[] { MultiFixMessages.UnreachableBlockCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("if (isValid && isFound) {\n"); //$NON-NLS-1$
		bld.append("    return 0;\n"); //$NON-NLS-1$

		if (!isEnabled(CleanUpConstants.UNREACHABLE_BLOCK)) {
			bld.append("} else if (isFound && isValid) {\n"); //$NON-NLS-1$
			bld.append("    return 1;\n"); //$NON-NLS-1$
		}

		bld.append("}\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.UNREACHABLE_BLOCK)) {
			bld.append("\n\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.UNREACHABLE_BLOCK)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement visited) {
				if (!ASTNodes.isExceptionExpected(visited)
						&& ASTNodes.isPassive(visited.getExpression())) {
					boolean hasNoUnreachableCodeBelow= ASTNodes.getNextStatement(visited) == null
							|| !ASTNodes.fallsThrough(visited.getThenStatement());
					IfStatement duplicateIf= visited;

					do {
						duplicateIf= ASTNodes.as(duplicateIf.getElseStatement(), IfStatement.class);

						if (duplicateIf == null
								|| !ASTNodes.isPassive(duplicateIf.getExpression())) {
							return true;
						}

						if ((hasNoUnreachableCodeBelow
								|| duplicateIf.getElseStatement() == null
								|| ASTNodes.fallsThrough(duplicateIf.getThenStatement())
								|| !ASTNodes.fallsThrough(duplicateIf.getElseStatement()))
								&& ASTNodes.isPassiveWithoutFallingThrough(duplicateIf.getExpression())
								&& ASTNodes.match(visited.getExpression(), duplicateIf.getExpression())) {
							rewriteOperations.add(new UnreachableBlockOperation(duplicateIf));
							return false;
						}

						hasNoUnreachableCodeBelow|= !ASTNodes.fallsThrough(duplicateIf.getThenStatement());
					} while (true);
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.UnreachableBlockCleanUp_description, unit,
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

	private static class UnreachableBlockOperation extends CompilationUnitRewriteOperation {
		private final IfStatement duplicateIf;

		public UnreachableBlockOperation(final IfStatement duplicateIf) {
			this.duplicateIf= duplicateIf;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.UnreachableBlockCleanUp_description, cuRewrite);

			if (duplicateIf.getElseStatement() == null) {
				rewrite.remove(duplicateIf, group);
			} else {
				ASTNodes.replaceButKeepComment(rewrite, duplicateIf, ASTNodes.createMoveTarget(rewrite, duplicateIf.getElseStatement()), group);
			}
		}
	}
}
