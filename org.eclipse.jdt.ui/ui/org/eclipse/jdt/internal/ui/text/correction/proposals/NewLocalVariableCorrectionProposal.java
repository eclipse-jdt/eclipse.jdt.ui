/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;

public class NewLocalVariableCorrectionProposal extends LinkedCorrectionProposal {

	public NewLocalVariableCorrectionProposal(NewLocalVariableCorrectionProposalCore core, Image image) {
		super(core.getName(), core.getCompilationUnit(), null, core.getRelevance(), image, core);
	}

	public NewLocalVariableCorrectionProposal(String label, ICompilationUnit cu, ASTNode node, int relevance, Image image) {
		super(label, cu, null, relevance, image, new NewLocalVariableCorrectionProposalCore(label, cu, node, relevance));
	}

}
