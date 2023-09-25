/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;

public class ReplaceCorrectionProposalCore extends CUCorrectionProposalCore {
	private String fReplacementString;
	private int fOffset;
	private int fLength;

	public ReplaceCorrectionProposalCore(String name, ICompilationUnit cu, int offset, int length, String replacementString, int relevance) {
		super(name, cu, relevance);
		fReplacementString= replacementString;
		fOffset= offset;
		fLength= length;
	}

	@Override
	public void addEdits(IDocument doc, TextEdit rootEdit) throws CoreException {
		super.addEdits(doc, rootEdit);
		TextEdit edit= new ReplaceEdit(fOffset, fLength, fReplacementString);
		rootEdit.addChild(edit);
	}
}