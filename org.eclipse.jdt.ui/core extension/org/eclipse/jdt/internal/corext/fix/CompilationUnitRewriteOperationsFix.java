/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to extend CompilationUnitRewriteOperationsFixCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class CompilationUnitRewriteOperationsFix extends CompilationUnitRewriteOperationsFixCore implements ILinkedFix {
	public static final String UNTOUCH_COMMENT= "untouchComment"; //$NON-NLS-1$

	public abstract static class CompilationUnitRewriteOperation extends CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation {

		public abstract void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException;

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			cuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode node) {
					if (Boolean.TRUE.equals(node.getProperty(UNTOUCH_COMMENT))) {
						return new SourceRange(node.getStartPosition(), node.getLength());
					}

					return super.computeSourceRange(node);
				}
			});
			rewriteAST(cuRewrite, (LinkedProposalModel)linkedModel);
		}
	}

	public CompilationUnitRewriteOperationsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		this(name, compilationUnit, new CompilationUnitRewriteOperation[] { operation });
		Assert.isNotNull(operation);
	}

	public CompilationUnitRewriteOperationsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations) {
		super(name, compilationUnit, operations);
		fLinkedProposalModel= new LinkedProposalModel();
	}

	public CompilationUnitRewriteOperationsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] operations) {
		super(name, compilationUnit, operations);
		fLinkedProposalModel= new LinkedProposalModel();
	}

	@Override
	public LinkedProposalModel getLinkedPositions() {
		if (!fLinkedProposalModel.hasLinkedPositions())
			return null;

		return (LinkedProposalModel)fLinkedProposalModel;
	}

}
