/*******************************************************************************
 * Copyright (c) 2024 Fabrice TIERCELIN and others.
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
 *     Red Hat Inc. - code extracted from ElseIfCleanUp
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class ElseIfFixCore extends CompilationUnitRewriteOperationsFixCore {

	private static class ElseIfOperation extends CompilationUnitRewriteOperation {
		private final IfStatement visited;
		private final IfStatement innerIf;

		public ElseIfOperation(final IfStatement visited, final IfStatement innerIf) {
			this.visited= visited;
			this.innerIf= innerIf;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_ElseIf_description, cuRewrite);

			rewrite.replace(visited.getElseStatement(), ASTNodes.createMoveTarget(rewrite, innerIf), group);
		}
	}

	public static ICleanUpFix createCleanUp(final CompilationUnit unit) {

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

		return new CompilationUnitRewriteOperationsFixCore(MultiFixMessages.CodeStyleCleanUp_ElseIf_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));

	}

	protected ElseIfFixCore(final String name, final CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
