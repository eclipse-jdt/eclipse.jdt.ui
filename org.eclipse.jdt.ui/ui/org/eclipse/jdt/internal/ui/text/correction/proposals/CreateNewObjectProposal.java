/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * <pre>
 * - propose create new local instance
 * </pre>
 */
public class CreateNewObjectProposal extends LinkedCorrectionProposal {

	public CreateNewObjectProposal(ICompilationUnit cu, Statement selectedNode, ITypeBinding typeNode, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		setDelegate(new CreateNewObjectProposalCore(cu, selectedNode, typeNode, relevance));
	}

	public CreateNewObjectProposal(ICompilationUnit cu, VariableDeclarationFragment variableDeclarationFragment, ITypeBinding typeNode, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		setDelegate(new CreateNewObjectProposalCore(cu, variableDeclarationFragment, typeNode, relevance));
	}

	public CreateNewObjectProposal(ICompilationUnit cu, VariableDeclarationFragment variableDeclarationFragment, IVariableBinding variableBinding, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		setDelegate(new CreateNewObjectProposalCore(cu, variableDeclarationFragment, variableBinding, relevance));
	}

	@Override
	public Image getImage() {
		return JavaPlugin.getImageDescriptorRegistry().get(
				new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE));
	}

	public boolean hasProposal() {
		return ((CreateNewObjectProposalCore)getDelegate()).hasProposal();
	}
}