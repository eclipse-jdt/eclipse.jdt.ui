/*******************************************************************************
 * Copyright (c) 2020, 2023 Fabrice TIERCELIN and others.
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
 *     Red Hat Inc. - refactor to NoStringCreationCleanUpCore
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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that removes a String instance from a String literal.
 */
public class NoStringCreationCleanUpCore extends AbstractMultiFix {
	public NoStringCreationCleanUpCore() {
		this(Collections.emptyMap());
	}

	public NoStringCreationCleanUpCore(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.NO_STRING_CREATION);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.NO_STRING_CREATION)) {
			return new String[] { MultiFixMessages.NoStringCreationCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.NO_STRING_CREATION)) {
			return """
				String bar = "foo";
				String newBar = bar.concat("abc");
				String cantChange = new String(possibleNullObject)
				"""; //$NON-NLS-1$
		}

		return """
			String bar = new String("foo");
			String newBar = (new String(bar)).concat("abc");
			String cantChange = new String(possibleNullObject)
			"""; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.NO_STRING_CREATION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final ClassInstanceCreation node) {
				if (ASTNodes.hasType(node, String.class.getCanonicalName()) && node.arguments().size() == 1) {
					Expression arg0= ASTNodes.getUnparenthesedExpression((Expression)node.arguments().get(0));
					while (arg0 instanceof ClassInstanceCreation c && ASTNodes.hasType(c, String.class.getCanonicalName()) &&
							c.arguments().size() == 1) {
						arg0= ASTNodes.getUnparenthesedExpression((Expression)c.arguments().get(0));
					}
					if (ASTNodes.hasType(arg0, String.class.getCanonicalName())) {
						if (arg0 instanceof StringLiteral || arg0 instanceof InfixExpression) {
							rewriteOperations.add(new NoStringCreationOperation(node, arg0));
							return false;
						} else if (arg0 instanceof MethodInvocation || arg0 instanceof SimpleName) {
							ASTNode parent= node.getParent();
							while (parent instanceof ParenthesizedExpression) {
								parent= parent.getParent();
							}
							if (parent instanceof Assignment || parent instanceof VariableDeclarationFragment) {
								return true;
							}
							if (parent instanceof MethodInvocation || parent instanceof FieldAccess) {
								rewriteOperations.add(new NoStringCreationOperation(node, arg0));
							}
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

		return new CompilationUnitRewriteOperationsFixCore(MultiFixMessages.NoStringCreationCleanUp_description, unit,
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

	private static class NoStringCreationOperation extends CompilationUnitRewriteOperation {
		private final ClassInstanceCreation node;
		private final Expression arg0;

		public NoStringCreationOperation(final ClassInstanceCreation node, final Expression arg0) {
			this.node= node;
			this.arg0= arg0;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.NoStringCreationCleanUp_description, cuRewrite);
			ASTNode replacement= ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, arg0));

			ASTNode nodeToReplace= node;
			while (nodeToReplace.getParent() instanceof ParenthesizedExpression) {
				nodeToReplace= nodeToReplace.getParent();
			}
			ASTNodes.replaceButKeepComment(rewrite, nodeToReplace, replacement, group);
		}
	}
}
