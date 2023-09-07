/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class AddArgumentCorrectionProposal extends LinkedCorrectionProposal {
	public AddArgumentCorrectionProposal(String label, ICompilationUnit cu, ASTNode callerNode, int[] insertIdx, ITypeBinding[] expectedTypes, int relevance) {
		super(label, cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		setDelegate(new AddArgumentCorrectionProposalCore(label, cu, callerNode, insertIdx, expectedTypes, relevance));
	}
}