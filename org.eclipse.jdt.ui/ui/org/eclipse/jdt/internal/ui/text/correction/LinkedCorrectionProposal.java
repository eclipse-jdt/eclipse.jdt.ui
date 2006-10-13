/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroup;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

/**
 * A proposal for quick fixes and quick assists that works on a AST rewriter and enters the
 * linked mode when the proposal is set up.
 * Either a rewriter is directly passed in the constructor or method {@link #getRewrite()} is overridden
 * to provide the AST rewriter that is evaluated to the document when the proposal is
 * applied.
 * @since 3.0
 */
public class LinkedCorrectionProposal extends ASTRewriteCorrectionProposal {

	/**
	 * Constructs a linked correction proposal.
	 * @param name The display name of the proposal.
	 * @param cu The compilation unit that is modified.
	 * @param rewrite The AST rewrite that is invoked when the proposal is applied
	 *  <code>null</code> can be passed if {@link #getRewrite()} is overridden.
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	public LinkedCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, rewrite, relevance, image);
	}

	/**
	 * Adds a linked position to be shown when the proposal is applied. All position with the
	 * same group id are linked.
	 * @param position The position to add.
	 * @param isFirst If set, the proposal is jumped to first.
	 * @param groupID The id of the group the proposal belongs to. All proposals in the same group
	 * are linked.
	 */
	public void addLinkedPosition(ITrackedNodePosition position, boolean isFirst, String groupID) {
		getLinkedProposalPositions().getPositionGroup(groupID, true).addPosition(position, isFirst);
	}

	/**
	 * Sets the end position of the linked mode to the end of the passed range.
	 * @param position The position that describes the end position of the linked mode.
	 */
	public void setEndPosition(ITrackedNodePosition position) {
		getLinkedProposalPositions().setEndPosition(position);
	}

	/**
	 * Adds a linked position proposal to the group with the given id.
	 * @param groupID The id of the group that should present the proposal
	 * @param proposal The string to propose.
	 * @param image The image to show for the position proposal or <code>null</code> if
	 * no image is desired.
	 */
	public void addLinkedPositionProposal(String groupID, String proposal, Image image) {
		addLinkedPositionProposal(groupID, proposal, proposal, image);
	}
	
	/**
	 * Adds a linked position proposal to the group with the given id.
	 * @param groupID The id of the group that should present the proposal
	 * 	@param displayString The name of the proposal
	 * @param proposal The string to insert.
	 * @param image The image to show for the position proposal or <code>null</code> if
	 * no image is desired.
	 */
	public void addLinkedPositionProposal(String groupID, String displayString, String proposal, Image image) {
		getLinkedProposalPositions().getPositionGroup(groupID, true).addProposal(new JavaLinkedModeProposal(displayString, image, proposal));
	}

	/**
	 * Adds a linked position proposal to the group with the given id.
	 * @param groupID The id of the group that should present the proposal
	 * @param type The binding to use as type name proposal.
	 */
	public void addLinkedPositionProposal(String groupID, ITypeBinding type) {
		getLinkedProposalPositions().getPositionGroup(groupID, true).addProposal(new JavaLinkedModeProposal(getCompilationUnit(), type));

	}
	
	private static final class JavaLinkedModeProposal extends LinkedProposalPositionGroup.Proposal {
		private final String fReplacementString;
		private final ITypeBinding fTypeProposal;
		private final ICompilationUnit fCompilationUnit;

		public JavaLinkedModeProposal(String displayString, Image image, String replacementString) {
			super(displayString, null, 10);
			fReplacementString= replacementString;
			fTypeProposal= null;
			fCompilationUnit=null;
		}

		public JavaLinkedModeProposal(ICompilationUnit unit, ITypeBinding typeProposal) {
			super(BindingLabelProvider.getBindingLabel(typeProposal, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_POST_QUALIFIED), null, 10);
			fReplacementString= typeProposal.getName();
			fTypeProposal= typeProposal;
			fCompilationUnit= unit;
			ImageDescriptor desc= BindingLabelProvider.getBindingImageDescriptor(fTypeProposal, BindingLabelProvider.DEFAULT_IMAGEFLAGS);
			if (desc != null) {
				setImage(JavaPlugin.getImageDescriptorRegistry().get(desc));
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.PositionGroup.Proposal#computeEdits(int, org.eclipse.jface.text.link.LinkedPosition, char, int, org.eclipse.jface.text.link.LinkedModeModel)
		 */
		public TextEdit computeEdits(int offset, LinkedPosition position, char trigger, int stateMask, LinkedModeModel model) throws CoreException {
			String replaceString= fReplacementString;
			ImportRewrite impRewrite= null;
			if (fTypeProposal != null) {
				impRewrite= StubUtility.createImportRewrite(fCompilationUnit, true);
				replaceString= impRewrite.addImport(fTypeProposal);
			}
			TextEdit edit= new ReplaceEdit(position.getOffset(), position.getLength(), replaceString);
			if (impRewrite == null)
				return edit;
				
			MultiTextEdit composedEdit= new MultiTextEdit();
			composedEdit.addChild(edit);
			composedEdit.addChild(impRewrite.rewriteImports(null));
			return composedEdit;
		}
	}

}
