/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;

import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;


/**
 * A template proposal.
 */
public class LinkedOpenDeclarationProposalCore extends CUCorrectionProposalCore {

	public static final String ASSIST_ID= "org.eclipse.jdt.ui.correction.showOriginalDeclaration.assist"; //$NON-NLS-1$

	private SimpleName fNode;
	private String fLabel;

	public LinkedOpenDeclarationProposalCore(String label, IInvocationContext context, SimpleName node) {
		super(label, context.getCompilationUnit(), IProposalRelevance.LINKED_NAMES_ASSIST);
		fLabel= label;
		fNode= node;
	}

	public String getLabel() {
		return fLabel;
	}

	public SimpleName getNode() {
		return fNode;
	}

	@Override
	public String getCommandId() {
		return ASSIST_ID;
	}

}
