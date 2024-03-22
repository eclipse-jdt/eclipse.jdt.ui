/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class NewVariableCorrectionProposal extends LinkedCorrectionProposal {
	public static final int LOCAL= NewVariableCorrectionProposalCore.LOCAL;

	public static final int FIELD= NewVariableCorrectionProposalCore.FIELD;

	public static final int PARAM= NewVariableCorrectionProposalCore.PARAM;

	public static final int CONST_FIELD= NewVariableCorrectionProposalCore.CONST_FIELD;

	public static final int ENUM_CONST= NewVariableCorrectionProposalCore.ENUM_CONST;

	public NewVariableCorrectionProposal(NewVariableCorrectionProposalCore core, Image image) {
		super(core.getName(), core.getCompilationUnit(), null, core.getRelevance(), image, core);
	}

	public NewVariableCorrectionProposal(String label, ICompilationUnit cu, int variableKind, SimpleName node, ITypeBinding senderBinding, int relevance, Image image) {
		super(label, cu, null, relevance, image, new NewVariableCorrectionProposalCore(label, cu, variableKind, node, senderBinding, relevance, false));
	}

	public int getVariableKind() {
		return ((NewVariableCorrectionProposalCore) getDelegate()).getVariableKind();
	}

}
