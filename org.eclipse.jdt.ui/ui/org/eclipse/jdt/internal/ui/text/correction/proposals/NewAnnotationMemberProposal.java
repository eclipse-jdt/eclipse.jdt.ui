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

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class NewAnnotationMemberProposal extends LinkedCorrectionProposal {

	public NewAnnotationMemberProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		setDelegate(new NewAnnotationMemberProposalCore(label, targetCU, invocationNode, binding, relevance));
	}
}