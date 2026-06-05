/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class SwitchCaseUnblockFixCore extends CompilationUnitRewriteOperationsFixCore {


	public static class SwitchExpressionUnwrapBlockOperation extends CompilationUnitRewriteOperation {

		private final Block block;

		public SwitchExpressionUnwrapBlockOperation(Block block) {
			this.block= block;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {

			final ASTRewrite rewrite= cuRewrite.getASTRewrite();
			final AST ast= rewrite.getAST();

			Statement stmt= (Statement) block.statements().get(0);
			Statement newStmt= null;
			if (stmt instanceof YieldStatement yieldStmt) {
				newStmt= ast.newExpressionStatement((Expression) rewrite.createCopyTarget(yieldStmt.getExpression()));
			} else {
				newStmt= (Statement)rewrite.createCopyTarget(stmt);
			}
			rewrite.replace(block, newStmt, null);
		}


	}

	public static SwitchCaseUnblockFixCore createFix(Block block) {
		CompilationUnit root= (CompilationUnit) block.getRoot();

		ASTNode parent= block.getParent();
		List<Statement> statements= new ArrayList<>();
		if (parent instanceof SwitchStatement switchStatement) {
			statements= switchStatement.statements();
		} else if (parent instanceof SwitchExpression switchExpression) {
			statements= switchExpression.statements();
		}

		if (statements.size() > 0 && statements.get(0) instanceof SwitchCase switchCase && switchCase.isSwitchLabeledRule()) {
			SwitchExpressionUnwrapBlockOperation op= new SwitchExpressionUnwrapBlockOperation(block);
			return new SwitchCaseUnblockFixCore(FixMessages.SwitchCaseUnblockFix_unwrap_case_block, root, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { op });
		}
		return null;
	}

	protected SwitchCaseUnblockFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
