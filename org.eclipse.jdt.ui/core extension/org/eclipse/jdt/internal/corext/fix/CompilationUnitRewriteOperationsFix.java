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

import org.eclipse.jdt.core.dom.CompilationUnit;

public class CompilationUnitRewriteOperationsFix extends CompilationUnitRewriteOperationsFixCore {
	public static final String UNTOUCH_COMMENT= CompilationUnitRewriteOperationsFixCore.UNTOUCH_COMMENT_PROPERTY;

	/**
	 * Logic moved down to lower bundle.
	 * Please use CompilationUnitRewriteOperationWithSourceRange instead
	 */
	@Deprecated
	public abstract static class CompilationUnitRewriteOperation extends CompilationUnitRewriteOperationWithSourceRange {
	}

	public CompilationUnitRewriteOperationsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationWithSourceRange operation) {
		this(name, compilationUnit, new CompilationUnitRewriteOperationWithSourceRange[] { operation });
		Assert.isNotNull(operation);
	}

	public CompilationUnitRewriteOperationsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationWithSourceRange[] operations) {
		this(name, compilationUnit, operations, new LinkedProposalModelCore());
	}

	public CompilationUnitRewriteOperationsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] operations) {
		this(name, compilationUnit, operations, new LinkedProposalModelCore());
	}

	public CompilationUnitRewriteOperationsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] operations, LinkedProposalModelCore proposalModel) {
		super(name, compilationUnit, operations);
		fLinkedProposalModel= proposalModel;
	}

	@Override
	public LinkedProposalModelCore getLinkedPositions() {
		return super.getLinkedPositions();
	}
}
