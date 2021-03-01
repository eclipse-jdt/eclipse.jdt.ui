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
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class PlainReplacementFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class PlainReplacementFinder extends ASTVisitor {
		private Pattern HAS_REGEX_CHARACTER= Pattern.compile("[\\\\\\[\\]\\{\\}\\(\\)\\*\\+\\?\\.\\^\\$\\|]"); //$NON-NLS-1$

		private List<PlainReplacementFixOperation> fResult;

		public PlainReplacementFinder(List<PlainReplacementFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final MethodInvocation visited) {
			if (ASTNodes.usesGivenSignature(visited, String.class.getCanonicalName(), "replaceAll", String.class.getCanonicalName(), String.class.getCanonicalName())) { //$NON-NLS-1$
				Object pattern= ((Expression) visited.arguments().get(0)).resolveConstantExpressionValue();
				Object replacement= ((Expression) visited.arguments().get(1)).resolveConstantExpressionValue();

				if (pattern instanceof String
						&& !HAS_REGEX_CHARACTER.matcher((String) pattern).find()
						&& replacement instanceof String
						&& ((String) replacement).equals(Matcher.quoteReplacement((String) replacement))) {
					fResult.add(new PlainReplacementFixOperation(visited));
					return false;
				}
			}

			return true;
		}
	}

	public static class PlainReplacementFixOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visited;

		public PlainReplacementFixOperation(final MethodInvocation visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PlainReplacementCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			rewrite.set(visited, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("replace"), group); //$NON-NLS-1$
		}
	}


	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<PlainReplacementFixOperation> operations= new ArrayList<>();
		PlainReplacementFinder finder= new PlainReplacementFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new PlainReplacementFixCore(FixMessages.PlainReplacementFix_use_plain_text, compilationUnit, ops);
	}

	protected PlainReplacementFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
