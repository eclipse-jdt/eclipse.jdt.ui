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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class AddTypeParameterProposal extends LinkedCorrectionProposal {

	public AddTypeParameterProposal(AddTypeParameterProposalCore core) {
		super("", core.getCompilationUnit(), null, core.getRelevance(), JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC), core); //$NON-NLS-1$
	}

	public AddTypeParameterProposal(ICompilationUnit targetCU, IBinding binding, CompilationUnit astRoot, String name, ITypeBinding[] bounds, int relevance) {
		super("", targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC), new AddTypeParameterProposalCore(targetCU, binding, astRoot, name, bounds, relevance)); //$NON-NLS-1$
	}
}
