/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class RenameNodeCorrectionProposal extends CUCorrectionProposal {
	public RenameNodeCorrectionProposal(RenameNodeCorrectionProposalCore core) {
		super(core.getName(), core.getCompilationUnit(), core.getRelevance(), JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE), core);
	}

	public RenameNodeCorrectionProposal(String name, ICompilationUnit cu, int offset, int length, String newName, int relevance) {
		super(name, cu, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE), new RenameNodeCorrectionProposalCore(name, cu, offset, length, newName, relevance));
	}
}
