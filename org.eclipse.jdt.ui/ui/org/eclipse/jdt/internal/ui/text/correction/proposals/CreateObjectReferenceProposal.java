/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * <pre>
 * - propose local instance
 * - propose field instance
 * </pre>
 */
public class CreateObjectReferenceProposal extends LinkedCorrectionProposal {

	public CreateObjectReferenceProposal(ICompilationUnit cu, ASTNode selectedNode, ITypeBinding typeNode, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		setDelegate(new CreateObjectReferenceProposalCore(cu, selectedNode, typeNode, relevance));
	}

	@Override
	public Image getImage() {
		return JavaPlugin.getImageDescriptorRegistry().get(
				new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE));
	}

	public boolean hasProposal() {
		return ((CreateObjectReferenceProposalCore)getDelegate()).hasProposal();
	}
}