/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class OptionalCorrectionProposal extends ASTRewriteCorrectionProposal {
	public static final String ADD_OPTIONAL_ID= OptionalCorrectionProposalCore.ADD_OPTIONAL_ID;
	public static final int OPTIONAL_EMPTY= OptionalCorrectionProposalCore.OPTIONAL_EMPTY;
	public static final int OPTIONAL_OF= OptionalCorrectionProposalCore.OPTIONAL_OF;
	public static final int OPTIONAL_OF_NULLABLE= OptionalCorrectionProposalCore.OPTIONAL_OF_NULLABLE;

	/**
	 * Creates a 'wrap in optional' correction proposal.
	 *
	 * @param label the display name of the proposal
	 * @param targetCU the compilation unit that is modified
	 * @param nodeToWrap the node to wrap in Optional
	 * @param relevance the relevance of this proposal
	 * @param correctionType 0= Optional.empty(), 1= Optional.of(), 2= Optional.ofNullable()
	 */
	public OptionalCorrectionProposal(String label, ICompilationUnit targetCU, Expression nodeToWrap, int relevance, int correctionType) {
		super(label, targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST));
		setDelegate(new OptionalCorrectionProposalCore(label, targetCU, nodeToWrap, relevance, correctionType));
	}
}
