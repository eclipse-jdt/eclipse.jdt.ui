/*******************************************************************************
 * Copyright (c) 2021 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.helper.AbstractSimplifyPlatformStatus;
import org.eclipse.jdt.internal.corext.fix.helper.StatusErrorSimplifyPlatformStatus;
import org.eclipse.jdt.internal.corext.fix.helper.StatusInfoSimplifyPlatformStatus;
import org.eclipse.jdt.internal.corext.fix.helper.StatusWarningSimplifyPlatformStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public enum SimplifyPlatformStatusFixCore {

	STATUSWARNING(new StatusWarningSimplifyPlatformStatus()),
	STATUSERROR(new StatusErrorSimplifyPlatformStatus()),
	STATUSINFO(new StatusInfoSimplifyPlatformStatus());

	AbstractSimplifyPlatformStatus<ASTNode> explicitencoding;

	@SuppressWarnings("unchecked")
	SimplifyPlatformStatusFixCore(AbstractSimplifyPlatformStatus<? extends ASTNode> explicitencoding) {
		this.explicitencoding= (AbstractSimplifyPlatformStatus<ASTNode>) explicitencoding;
	}

	public String getPreview(boolean i) {
		return explicitencoding.getPreview(i);
	}

	/**
	 * Compute set of CompilationUnitRewriteOperation to refactor supported
	 * situations using platform status instantiation
	 *
	 * @param compilationUnit unit to search in
	 * @param operations      set of all CompilationUnitRewriteOperations created
	 *                        already
	 * @param nodesprocessed  list to remember nodes already processed
	 * @throws CoreException
	 */
	public void findOperations(final CompilationUnit compilationUnit,
			final Set<CompilationUnitRewriteOperation> operations, final Set<ASTNode> nodesprocessed)
					throws CoreException {
		explicitencoding.find(this, compilationUnit, operations, nodesprocessed);
	}

	public CompilationUnitRewriteOperation rewrite(final ClassInstanceCreation visited) {
		return new CompilationUnitRewriteOperation() {
			@Override
			public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel)
					throws CoreException {
				TextEditGroup group= createTextEditGroup(
						Messages.format(MultiFixMessages.PlatformStatusCleanUp_description,
								new Object[] { SimplifyPlatformStatusFixCore.this.toString() }),
						cuRewrite);
				cuRewrite.getASTRewrite().setTargetSourceRangeComputer(computer);
				explicitencoding.rewrite(SimplifyPlatformStatusFixCore.this, visited, cuRewrite, group);
			}
		};
	}

	final static TargetSourceRangeComputer computer= new TargetSourceRangeComputer() {
		@Override
		public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
			if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
				return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
			}
			return super.computeSourceRange(nodeWithComment);
		}
	};
}
