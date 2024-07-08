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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class TypeChangeCorrectionProposal extends LinkedCorrectionProposal {

	public TypeChangeCorrectionProposal(ICompilationUnit targetCU, IBinding binding, CompilationUnit astRoot, ITypeBinding newType, boolean offerSuperTypeProposals, int relevance) {
		this(targetCU, binding, astRoot, newType, false, offerSuperTypeProposals, relevance);
	}

	//This needs to be used to convert a given type to var type.
	public TypeChangeCorrectionProposal(ICompilationUnit targetCU, IBinding binding, CompilationUnit astRoot, ITypeBinding oldType, int relevance) {
		this(targetCU, binding, astRoot, oldType, true, false, relevance);
	}

	private TypeChangeCorrectionProposal(ICompilationUnit targetCU, IBinding binding, CompilationUnit astRoot, ITypeBinding newType, boolean isNewTypeVar, boolean offerSuperTypeProposals,
			int relevance) {
		super("", targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE), //$NON-NLS-1$
				new TypeChangeCorrectionProposalCore(targetCU, binding, astRoot, newType, isNewTypeVar, offerSuperTypeProposals, relevance));
	}

	public TypeChangeCorrectionProposal(ICompilationUnit targetCU, ASTNode nodeToChange, CompilationUnit astRoot, ITypeBinding variableTypeBinding, int relevance) {
		super("", targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE), //$NON-NLS-1$
				new TypeChangeCorrectionProposalCore(targetCU, nodeToChange, astRoot, variableTypeBinding, relevance));
	}
}
