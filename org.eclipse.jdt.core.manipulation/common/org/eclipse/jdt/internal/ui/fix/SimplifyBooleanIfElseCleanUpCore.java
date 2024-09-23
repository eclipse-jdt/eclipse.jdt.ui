/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.PatternInstanceofExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperationWithSourceRange;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class SimplifyBooleanIfElseCleanUpCore extends AbstractMultiFix {

	public SimplifyBooleanIfElseCleanUpCore() {
		this(Collections.emptyMap());
	}

	public SimplifyBooleanIfElseCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.SIMPLIFY_BOOLEAN_IF_ELSE);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.SIMPLIFY_BOOLEAN_IF_ELSE)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_SimplifyBooleanIfElse_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		if (isEnabled(CleanUpConstants.SIMPLIFY_BOOLEAN_IF_ELSE)) {
			bld.append("return i > 0;\n"); //$NON-NLS-1$
		} else {
			bld.append("if (i > 0) {\n"); //$NON-NLS-1$
			bld.append("    return true;\n"); //$NON-NLS-1$
			bld.append("} else {\n"); //$NON-NLS-1$
			bld.append("    return false;\n"); //$NON-NLS-1$
			bld.append("}\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		// TODO Auto-generated method stub
		return false;
	}

	public static enum SimplifyStatus {
		INVALID, VALID_THEN_TRUE, VALID_ELSE_TRUE
	}

	public static SimplifyStatus verifyBooleanIfElse(final IfStatement ifStatement) {
		if (ifStatement.getElseStatement() == null || ifStatement.getExpression() instanceof PatternInstanceofExpression) {
			return SimplifyStatus.INVALID;
		}

		boolean thenValue= true;

		Statement thenStatement= ifStatement.getThenStatement();
		if (thenStatement instanceof ReturnStatement returnStatement) {
			if (returnStatement.getExpression() instanceof BooleanLiteral literal) {
				thenValue= literal.booleanValue();
			}
		} else if (thenStatement instanceof Block block && block.statements().size() == 1
				&& block.statements().get(0) instanceof ReturnStatement returnStatement) {
			if (returnStatement.getExpression() instanceof BooleanLiteral literal) {
				thenValue= literal.booleanValue();
			} else {
				return SimplifyStatus.INVALID;
			}
		} else {
			return SimplifyStatus.INVALID;
		}

		Statement elseStatement= ifStatement.getElseStatement();
		if (elseStatement instanceof ReturnStatement returnStatement) {
			if (returnStatement.getExpression() instanceof BooleanLiteral literal) {
				if (literal.booleanValue() == thenValue) {
					return SimplifyStatus.INVALID;
				}
			}
		} else if (elseStatement instanceof Block block && block.statements().size() == 1
				&& block.statements().get(0) instanceof ReturnStatement returnStatement) {
			if (returnStatement.getExpression() instanceof BooleanLiteral literal) {
				if (literal.booleanValue() == thenValue) {
					return SimplifyStatus.INVALID;
				}
			} else {
				return SimplifyStatus.INVALID;
			}
		} else {
			return SimplifyStatus.INVALID;
		}
		return thenValue == true ? SimplifyStatus.VALID_THEN_TRUE : SimplifyStatus.VALID_ELSE_TRUE;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.SIMPLIFY_BOOLEAN_IF_ELSE)) {
			return null;
		}

		final List<CompilationUnitRewriteOperationWithSourceRange> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement visited) {
				SimplifyStatus status= verifyBooleanIfElse(visited);
				if (status != SimplifyStatus.INVALID) {
					rewriteOperations.add(new SimplifyBooleanIfElseOperation(visited, status));
				}
				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFixCore(MultiFixMessages.CodeStyleCleanUp_SimplifyBooleanIfElse_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperationWithSourceRange[0]));
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	private static class SimplifyBooleanIfElseOperation extends CompilationUnitRewriteOperationWithSourceRange {
		private final IfStatement visited;
		private final SimplifyStatus status;

		public SimplifyBooleanIfElseOperation(final IfStatement visited, final SimplifyStatus status) {
			this.visited= visited;
			this.status= status;
		}

		@Override
		public void rewriteASTInternal(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_SimplifyBooleanIfElse_description, cuRewrite);

			CompilationUnit cu= (CompilationUnit) visited.getRoot();
			ICompilationUnit icu= (ICompilationUnit) cu.getJavaElement();
			NLSLine nlsLine= CleanUpNLSUtils.scanCurrentLine(icu, visited.getExpression());
			StringBuilder nlsBuffer= new StringBuilder();
			if (nlsLine != null) {
				NLSElement[] nlsElements= nlsLine.getElements();
				for (int i= 0; i < nlsElements.length; ++i) {
					NLSElement element= nlsElements[i];
					if (element.hasTag()) {
						nlsBuffer.append(" //$NON-NLS-" + (i + 1) + "$"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			ReturnStatement newReturn= null;
			String ifExpression= icu.getBuffer().getText(visited.getExpression().getStartPosition(), visited.getExpression().getLength());
			if (status == SimplifyStatus.VALID_THEN_TRUE) {
				newReturn= (ReturnStatement) rewrite.createStringPlaceholder("return " + ifExpression + ";" + nlsBuffer.toString(), ASTNode.RETURN_STATEMENT); //$NON-NLS-1$ //$NON-NLS-2$
			} else { // otherwise we need to reverse the if expression so it returns the right value
				newReturn= (ReturnStatement) rewrite.createStringPlaceholder("return !(" + ifExpression + ");" + //$NON-NLS-1$ //$NON-NLS-2$
						nlsBuffer.toString(), ASTNode.RETURN_STATEMENT);
			}
			rewrite.replace(visited, newReturn, group);
		}

	}

}
