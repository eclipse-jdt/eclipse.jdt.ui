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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

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
 * A fix that uses the <code>else if</code> pseudo keyword.
 */
public class ElseIfCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public ElseIfCleanUp() {
		this(Collections.emptyMap());
	}

	public ElseIfCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.ELSE_IF);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.ELSE_IF)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_ElseIf_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.ELSE_IF)) {
			return "" //$NON-NLS-1$
					+ "if (isValid) {\n" //$NON-NLS-1$
					+ "  System.out.println(isValid);\n" //$NON-NLS-1$
					+ "} else if (isEnabled) {\n" //$NON-NLS-1$
					+ "  System.out.println(isEnabled);\n" //$NON-NLS-1$
					+ "}\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (isValid) {\n" //$NON-NLS-1$
				+ "  System.out.println(isValid);\n" //$NON-NLS-1$
				+ "} else {\n" //$NON-NLS-1$
				+ "  if (isEnabled) {\n" //$NON-NLS-1$
				+ "    System.out.println(isEnabled);\n" //$NON-NLS-1$
				+ "  }\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.ELSE_IF)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement visited) {
				Statement elseStatement= visited.getElseStatement();

				if (elseStatement instanceof Block) {
					IfStatement innerIf= ASTNodes.as(elseStatement, IfStatement.class);

					if (innerIf != null) {
						rewriteOperations.add(new ElseIfOperation(visited, innerIf));
						return false;
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CodeStyleCleanUp_ElseIf_description, unit,
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

	private static class ElseIfOperation extends CompilationUnitRewriteOperation {
		private final IfStatement visited;
		private final IfStatement innerIf;

		public ElseIfOperation(final IfStatement visited, final IfStatement innerIf) {
			this.visited= visited;
			this.innerIf= innerIf;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_ElseIf_description, cuRewrite);

			rewrite.replace(visited.getElseStatement(), ASTNodes.createMoveTarget(rewrite, innerIf), group);
		}
	}
}
