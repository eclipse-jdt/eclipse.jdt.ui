/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - split into LinkedProposalPositionGroupCore in
 *                    core manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.manipulation.CodeStyleConfiguration;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class LinkedProposalPositionGroup extends LinkedProposalPositionGroupCore {

	public static class Proposal extends ProposalCore {

		private Image fImage;

		public Proposal(String displayString, Image image, int relevance) {
			super(displayString, relevance);
			fImage= image;
		}

		public Image getImage() {
			return fImage;
		}

		public void setImage(Image image) {
			fImage= image;
		}
	}


	private static final class JavaLinkedModeProposal extends Proposal {
		private final ITypeBinding fTypeProposal;
		private final ICompilationUnit fCompilationUnit;

		public JavaLinkedModeProposal(ICompilationUnit unit, ITypeBinding typeProposal, int relevance) {
			super(BindingLabelProviderCore.getBindingLabel(typeProposal, JavaElementLabelsCore.ALL_DEFAULT | JavaElementLabelsCore.ALL_POST_QUALIFIED), null, relevance);
			fTypeProposal= typeProposal;
			fCompilationUnit= unit;

			ImageDescriptor desc= BindingLabelProvider.getBindingImageDescriptor(typeProposal, BindingLabelProvider.DEFAULT_IMAGEFLAGS);
			if (desc != null) {
				setImage(JavaPlugin.getImageDescriptorRegistry().get(desc));
			}
		}

		@Override
		public TextEdit computeEdits(int offset, LinkedPosition position, char trigger, int stateMask, LinkedModeModel model) throws CoreException {
			ImportRewrite impRewrite= CodeStyleConfiguration.createImportRewrite(fCompilationUnit, true);
			String replaceString= impRewrite.addImport(fTypeProposal);

			MultiTextEdit composedEdit= new MultiTextEdit();
			composedEdit.addChild(new ReplaceEdit(position.getOffset(), position.getLength(), replaceString));
			composedEdit.addChild(impRewrite.rewriteImports(null));
			return composedEdit;
		}
	}


	private final List<Proposal> fProposals;


	public LinkedProposalPositionGroup(String groupID) {
		super(groupID);
		fProposals= new ArrayList<>();
	}

	public void addProposal(Proposal proposal) {
		fProposals.add(proposal);
	}

	public void addProposal(String displayString, Image image, int relevance) {
		addProposal(new Proposal(displayString, image, relevance));
	}

	@Override
	public void addProposal(ITypeBinding type, ICompilationUnit cu, int relevance) {
		addProposal(new JavaLinkedModeProposal(cu, type, relevance));
	}

	@Override
	public Proposal[] getProposals() {
		return fProposals.toArray(new Proposal[fProposals.size()]);
	}

}
