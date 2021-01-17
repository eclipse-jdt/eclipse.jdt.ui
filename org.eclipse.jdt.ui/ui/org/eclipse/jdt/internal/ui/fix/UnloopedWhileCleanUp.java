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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that replaces a <code>while</code> loop that always terminates during the first iteration by an <code>if</code>:
 * <ul>
 * <li>The loop should not contain any <code>continue</code> statement,</li>
 * <li>The loop should only contain <code>break</code> statements without statements after.</li>
 * </ul>
 */
public class UnloopedWhileCleanUp extends AbstractMultiFix {
	private static class BreakVisitor extends InterruptibleVisitor {
		private final WhileStatement root;
		private final List<BreakStatement> breaks= new ArrayList<>();
		private boolean canBeRefactored= true;

		public BreakVisitor(final WhileStatement root) {
			this.root= root;
		}

		public List<BreakStatement> getBreaks() {
			return breaks;
		}

		public boolean canBeRefactored() {
			return canBeRefactored;
		}

		@Override
		public boolean visit(final BreakStatement aBreak) {
			if (aBreak.getLabel() != null) {
				canBeRefactored= false;
				return interruptVisit();
			}

			Statement parent= aBreak;
			do {
				parent= ASTNodes.getTypedAncestor(parent, Statement.class);
			} while (parent != root && ASTNodes.getNextSiblings(parent).isEmpty());

			if (parent != root) {
				canBeRefactored= false;
				return interruptVisit();
			}

			breaks.add(aBreak);

			return true;
		}

		@Override
		public boolean visit(final WhileStatement visited) {
			return root.equals(visited);
		}

		@Override
		public boolean visit(final DoStatement visited) {
			return false;
		}

		@Override
		public boolean visit(final ForStatement visited) {
			return false;
		}

		@Override
		public boolean visit(final EnhancedForStatement visited) {
			return false;
		}

		@Override
		public boolean visit(final SwitchStatement visited) {
			return false;
		}

		@Override
		public boolean visit(final AnonymousClassDeclaration visited) {
			return false;
		}

		@Override
		public boolean visit(final LambdaExpression visited) {
			return false;
		}
	}

	private static class ContinueVisitor extends InterruptibleVisitor {
		private final WhileStatement root;
		private boolean canBeRefactored= true;

		public ContinueVisitor(final WhileStatement root) {
			this.root= root;
		}

		public boolean canBeRefactored() {
			return canBeRefactored;
		}

		@Override
		public boolean visit(final ContinueStatement visited) {
			canBeRefactored= false;
			return interruptVisit();
		}

		@Override
		public boolean visit(final WhileStatement visited) {
			return root.equals(visited);
		}

		@Override
		public boolean visit(final DoStatement visited) {
			return false;
		}

		@Override
		public boolean visit(final ForStatement visited) {
			return false;
		}

		@Override
		public boolean visit(final EnhancedForStatement visited) {
			return false;
		}

		@Override
		public boolean visit(final AnonymousClassDeclaration visited) {
			return false;
		}

		@Override
		public boolean visit(final LambdaExpression visited) {
			return false;
		}
	}

	public UnloopedWhileCleanUp() {
		this(Collections.emptyMap());
	}

	public UnloopedWhileCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.UNLOOPED_WHILE);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.UNLOOPED_WHILE)) {
			return new String[] { MultiFixMessages.UnloopedWhileCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.UNLOOPED_WHILE)) {
			bld.append("if (isValid) {\n"); //$NON-NLS-1$
		} else {
			bld.append("while (isValid) {\n"); //$NON-NLS-1$
		}

		bld.append("    System.out.println(\"foo\");\n"); //$NON-NLS-1$
		bld.append("    return;\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.UNLOOPED_WHILE)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final WhileStatement visited) {
				if (ASTNodes.fallsThrough(visited.getBody()) && !Boolean.TRUE.equals(ASTNodes.peremptoryValue(visited.getExpression()))) {
					ContinueVisitor continueVisitor= new ContinueVisitor(visited);
					continueVisitor.traverseNodeInterruptibly(visited);

					if (continueVisitor.canBeRefactored()) {
						BreakVisitor breakVisitor= new BreakVisitor(visited);
						breakVisitor.traverseNodeInterruptibly(visited);

						if (breakVisitor.canBeRefactored()) {
							rewriteOperations.add(new UnloopedWhileOperation(visited, breakVisitor));
							return false;
						}
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.UnloopedWhileCleanUp_description, unit,
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

	private static class UnloopedWhileOperation extends CompilationUnitRewriteOperation {
		private final WhileStatement visited;
		private final BreakVisitor breakVisitor;

		public UnloopedWhileOperation(final WhileStatement visited, final BreakVisitor breakVisitor) {
			this.visited= visited;
			this.breakVisitor= breakVisitor;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.UnloopedWhileCleanUp_description, cuRewrite);

			for (BreakStatement breakStatement : breakVisitor.getBreaks()) {
				if (ASTNodes.canHaveSiblings(breakStatement) || breakStatement.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
					rewrite.remove(breakStatement, group);
				} else {
					ASTNodes.replaceButKeepComment(rewrite, breakStatement, ast.newBlock(), group);
				}
			}

			IfStatement newIfStatement= ast.newIfStatement();
			newIfStatement.setExpression(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(visited.getExpression())));
			newIfStatement.setThenStatement(ASTNodes.createMoveTarget(rewrite, visited.getBody()));

			ASTNodes.replaceButKeepComment(rewrite, visited, newIfStatement, group);
		}
	}
}
