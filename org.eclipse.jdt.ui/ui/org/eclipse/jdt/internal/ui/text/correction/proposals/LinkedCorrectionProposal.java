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

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroup;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroup.Proposal;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroupCore.ProposalCore;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.viewsupport.LinkedProposalModelPresenter;


/**
 * A proposal for quick fixes and quick assists that works on a AST rewriter and enters the linked
 * mode when the proposal is set up. Either a rewriter is directly passed in the constructor or
 * method {@link #getRewrite()} is overridden to provide the AST rewriter that is evaluated to the
 * document when the proposal is applied.
 *
 * @since 3.0
 */
public class LinkedCorrectionProposal extends ASTRewriteCorrectionProposal implements ILinkedCorrectionProposalCore {


	/**
	 * Constructs a linked correction proposal.
	 *
	 * @param name The display name of the proposal.
	 * @param cu The compilation unit that is modified.
	 * @param rewrite The AST rewrite that is invoked when the proposal is applied <code>null</code>
	 *            can be passed if {@link #getRewrite()} is overridden.
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * @param delegate The delegate instance image is desired.
	 */
	public LinkedCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image, LinkedCorrectionProposalCore delegate) {
		super(name, cu, rewrite, relevance, image, delegate);
	}

	/**
	 * Constructs a linked correction proposal.
	 *
	 * @param name The display name of the proposal.
	 * @param cu The compilation unit that is modified.
	 * @param rewrite The AST rewrite that is invoked when the proposal is applied <code>null</code>
	 *            can be passed if {@link #getRewrite()} is overridden.
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no image
	 *            is desired.
	 */
	public LinkedCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, rewrite, relevance, image, new LinkedCorrectionProposalCore(name, cu, rewrite, relevance));
	}

	public LinkedCorrectionProposal(LinkedCorrectionProposalCore core, Image image) {
		super(core.getName(), core.getCompilationUnit(), null, core.getRelevance(), image, core);
	}


	/* public only for tests. */
	public LinkedProposalModelCore getLinkedProposalModel() {
		return ((LinkedCorrectionProposalCore) getDelegate()).getLinkedProposalModel();
	}

	public void setLinkedProposalModel(LinkedProposalModelCore model) {
		((LinkedCorrectionProposalCore) getDelegate()).setLinkedProposalModel(model);
	}

	/**
	 * Adds a linked position to be shown when the proposal is applied. All positions with the same
	 * group id are linked.
	 *
	 * @param position The position to add.
	 * @param isFirst If set, the proposal is jumped to first.
	 * @param groupID The id of the group the proposal belongs to. All proposals in the same group
	 *            are linked.
	 */
	public void addLinkedPosition(ITrackedNodePosition position, boolean isFirst, String groupID) {
		((LinkedCorrectionProposalCore) getDelegate()).addLinkedPosition(position, isFirst, groupID);
	}

	/**
	 * Adds a linked position to be shown when the proposal is applied. All positions with the same
	 * group id are linked.
	 *
	 * @param position The position to add.
	 * @param sequenceRank The sequence rank, see TODO.
	 * @param groupID The id of the group the proposal belongs to. All proposals in the same group
	 *            are linked.
	 */
	public void addLinkedPosition(ITrackedNodePosition position, int sequenceRank, String groupID) {
		((LinkedCorrectionProposalCore) getDelegate()).addLinkedPosition(position, sequenceRank, groupID);
	}

	/**
	 * Sets the end position of the linked mode to the end of the passed range.
	 *
	 * @param position The position that describes the end position of the linked mode.
	 */
	public void setEndPosition(ITrackedNodePosition position) {
		((LinkedCorrectionProposalCore) getDelegate()).setEndPosition(position);
	}

	/**
	 * Adds a linked position proposal to the group with the given id.
	 *
	 * @param groupID The id of the group that should present the proposal
	 * @param proposal The string to propose.
	 * @param image The image to show for the position proposal or <code>null</code> if no image is
	 *            desired.
	 */
	public void addLinkedPositionProposal(String groupID, String proposal, Image image) {
		ProposalCore p= new Proposal(proposal, image, 10);
		getLinkedProposalModel().getPositionGroup(groupID, true).addProposal(p);
	}

	public void addLinkedPositionProposal(String groupID, String proposal) {
		((LinkedCorrectionProposalCore) getDelegate()).addLinkedPositionProposal(groupID, proposal);
	}

	/**
	 * Adds a linked position proposal to the group with the given id.
	 *
	 * @param groupID The id of the group that should present the proposal
	 * @param displayString The name of the proposal
	 * @param proposal The string to insert.
	 * @param image The image to show for the position proposal or <code>null</code> if no image is
	 *            desired.
	 * @deprecated use {@link #addLinkedPositionProposal(String, String, Image)} instead
	 */
	@Deprecated
	public void addLinkedPositionProposal(String groupID, String displayString, String proposal, Image image) {
		addLinkedPositionProposal(groupID, proposal, image);
	}

	/**
	 * Adds a linked position proposal to the group with the given id.
	 *
	 * @param groupID The id of the group that should present the proposal
	 * @param type The binding to use as type name proposal.
	 */
	public void addLinkedPositionProposal(String groupID, ITypeBinding type) {
		getLinkedProposalModel().getPositionGroup(groupID, true).addProposal(type, getCompilationUnit(), 10);
	}

	@Override
	protected void performChange(IEditorPart part, IDocument document) throws CoreException {
		try {
			super.performChange(part, document);
			if (part == null) {
				return;
			}

			if (((LinkedCorrectionProposalCore) getDelegate()).fLinkedProposalModel != null) {
				if (((LinkedCorrectionProposalCore) getDelegate()).fLinkedProposalModel.hasLinkedPositions() && part instanceof JavaEditor) {
					// enter linked mode
					ITextViewer viewer= ((JavaEditor) part).getViewer();
					new LinkedProposalModelPresenter().enterLinkedMode(viewer, part, didOpenEditor(), ((LinkedCorrectionProposalCore) getDelegate()).fLinkedProposalModel);
				} else if (part instanceof ITextEditor) {
					LinkedProposalPositionGroup.PositionInformation endPosition= ((LinkedCorrectionProposalCore) getDelegate()).fLinkedProposalModel.getEndPosition();
					if (endPosition != null) {
						// select a result
						int pos= endPosition.getOffset() + endPosition.getLength();
						((ITextEditor) part).selectAndReveal(pos, 0);
					}
				}
			}
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		}
	}

}
