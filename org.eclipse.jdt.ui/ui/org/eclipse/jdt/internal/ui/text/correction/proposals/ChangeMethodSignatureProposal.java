/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;

import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.ChangeDescription;

public class ChangeMethodSignatureProposal extends LinkedCorrectionProposal {
	public ChangeMethodSignatureProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, IMethodBinding binding, ChangeDescription[] paramChanges, ChangeDescription[] exceptionChanges, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		setDelegate(new ChangeMethodSignatureProposalCore(label, targetCU, invocationNode, binding, paramChanges, exceptionChanges, relevance));
	}


	public String getParamNameGroupId(int idx) {
		return ((ChangeMethodSignatureProposalCore)getDelegate()).getParamNameGroupId(idx);
	}

	public String getParamTypeGroupId(int idx) {
		return ((ChangeMethodSignatureProposalCore)getDelegate()).getParamTypeGroupId(idx);
	}

	public String getExceptionTypeGroupId(int idx) {
		return ((ChangeMethodSignatureProposalCore)getDelegate()).getExceptionTypeGroupId(idx);
	}
}