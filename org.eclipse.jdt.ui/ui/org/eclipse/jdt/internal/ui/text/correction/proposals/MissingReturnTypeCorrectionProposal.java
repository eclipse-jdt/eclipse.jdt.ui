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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class MissingReturnTypeCorrectionProposal extends LinkedCorrectionProposal {
	public MissingReturnTypeCorrectionProposal(ICompilationUnit cu, MethodDeclaration decl, ReturnStatement existingReturn, int relevance) {
		super("", cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE), new MissingReturnTypeCorrectionProposalCore(cu, decl, existingReturn, relevance)); //$NON-NLS-1$
	}

	public MissingReturnTypeCorrectionProposal(MissingReturnTypeCorrectionProposalCore delegate) {
		super("", delegate.getCompilationUnit(), null, delegate.getRelevance(), JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE), delegate); //$NON-NLS-1$	}
	}
	public MissingReturnTypeCorrectionProposal(ICompilationUnit cu, int relevance,
			MissingReturnTypeCorrectionProposalCore delegate) {
		super("", cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE), delegate); //$NON-NLS-1$	}
	}
}
