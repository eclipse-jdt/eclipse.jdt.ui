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
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [quick fix] proposes wrong cast from Object to primitive int - https://bugs.eclipse.org/bugs/show_bug.cgi?id=100593
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class CastCorrectionProposal extends LinkedCorrectionProposal {

	public static final String ADD_CAST_ID= CastCorrectionProposalCore.ADD_CAST_ID;

	/**
	 * Creates a cast correction proposal.
	 *
	 * @param label the display name of the proposal
	 * @param targetCU the compilation unit that is modified
	 * @param nodeToCast the node to cast
	 * @param castType the type to cast to, may be <code>null</code>
	 * @param relevance the relevance of this proposal
	 */
	public CastCorrectionProposal(String label, ICompilationUnit targetCU, Expression nodeToCast, ITypeBinding castType, int relevance) {
		super(label, targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST), new CastCorrectionProposalCore(label, targetCU, nodeToCast, castType, relevance));
		setCommandId(ADD_CAST_ID);
	}

	public CastCorrectionProposal(CastCorrectionProposalCore core) {
		super(core.getName(), core.getCompilationUnit(), null, core.getRelevance(), JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST), core);
		setCommandId(ADD_CAST_ID);
	}

}
