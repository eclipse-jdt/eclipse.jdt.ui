/*******************************************************************************
 * Copyright (c) 2022 Fabrice TIERCELIN and others.
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
 *     Red Hat - moved here from jdt.ui project
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;

/**
 * A fix that removes the second <code>substring()</code> parameter if this parameter is the length of the string:
 * <ul>
 * <li>It must reference the same expression,</li>
 * <li>The expression must be passive.</li>
 * </ul>
 */
public class SubstringCleanUpCore extends AbstractMultiFixCore {
	public SubstringCleanUpCore() {
		this(Collections.emptyMap());
	}

	public SubstringCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		boolean requireAST= isEnabled(CleanUpConstants.SUBSTRING);
		return new CleanUpRequirementsCore(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.SUBSTRING)) {
			return new String[] { MultiFixMessages.SubstringCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.SUBSTRING)) {
			return "String shortenedText = text.substring(2);\n"; //$NON-NLS-1$
		}

		return "String shortenedText = text.substring(2, text.length());\n"; //$NON-NLS-1$
	}

	@Override
	public ICleanUpFixCore createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.SUBSTRING)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final MethodInvocation visited) {
				if (ASTNodes.usesGivenSignature(visited, String.class.getCanonicalName(), "substring", int.class.getCanonicalName(), int.class.getCanonicalName())) { //$NON-NLS-1$
					MethodInvocation endIndex= ASTNodes.as((Expression) visited.arguments().get(1), MethodInvocation.class);

					if (endIndex != null
							&& endIndex.getExpression() != null
							&& ASTNodes.usesGivenSignature(endIndex, String.class.getCanonicalName(), "length") //$NON-NLS-1$
							&& ASTNodes.match(visited.getExpression(), endIndex.getExpression())
							&& ASTNodes.isPassive(visited.getExpression())) {
				rewriteOperations.add(new SubstringOperation(visited));
						return false;
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFixCore(MultiFixMessages.SubstringCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}
	@Override
	public ICleanUpFixCore createFix(CompilationUnit unit, IProblemLocationCore[] problems) throws CoreException {
		return createFix(unit);
	}


	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocationCore problem) {
		return false;
	}

	private static class SubstringOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visited;

		public SubstringOperation(final MethodInvocation visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.SubstringCleanUp_description, cuRewrite);

			rewrite.remove((ASTNode) visited.arguments().get(1), group);
		}
	}
}
