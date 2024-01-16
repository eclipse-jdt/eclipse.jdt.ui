/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroupCore.ProposalCore;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;

public class LinkedCorrectionProposalCore extends ASTRewriteCorrectionProposalCore implements ILinkedCorrectionProposalCore {
	public LinkedProposalModelCore fLinkedProposalModel;

	public LinkedCorrectionProposalCore(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		super(name, cu, rewrite, relevance);
		fLinkedProposalModel= null;
	}

	/* public only for tests. */
	public LinkedProposalModelCore getLinkedProposalModel() {
		if (fLinkedProposalModel == null) {
			fLinkedProposalModel= new LinkedProposalModelCore();
		}
		return fLinkedProposalModel;
	}

	public void setLinkedProposalModel(LinkedProposalModelCore model) {
		fLinkedProposalModel= model;
	}

	/**
	 * Adds a linked position to be shown when the proposal is applied. All positions with the
	 * same group id are linked.
	 * @param position The position to add.
	 * @param isFirst If set, the proposal is jumped to first.
	 * @param groupID The id of the group the proposal belongs to. All proposals in the same group
	 * are linked.
	 */
	public void addLinkedPosition(ITrackedNodePosition position, boolean isFirst, String groupID) {
		getLinkedProposalModel().getPositionGroup(groupID, true).addPosition(position, isFirst);
	}

	/**
	 * Adds a linked position to be shown when the proposal is applied. All positions with the
	 * same group id are linked.
	 * @param position The position to add.
	 * @param sequenceRank The sequence rank, see TODO.
	 * @param groupID The id of the group the proposal belongs to. All proposals in the same group
	 * are linked.
	 */
	public void addLinkedPosition(ITrackedNodePosition position, int sequenceRank, String groupID) {
		getLinkedProposalModel().getPositionGroup(groupID, true).addPosition(position, sequenceRank);
	}

	/**
	 * Sets the end position of the linked mode to the end of the passed range.
	 * @param position The position that describes the end position of the linked mode.
	 */
	public void setEndPosition(ITrackedNodePosition position) {
		getLinkedProposalModel().setEndPosition(position);
	}

	/**
	 * Adds a linked position proposal to the group with the given id.
	 * @param groupID The id of the group that should present the proposal
	 * @param type The binding to use as type name proposal.
	 */
	public void addLinkedPositionProposal(String groupID, ITypeBinding type) {
		getLinkedProposalModel().getPositionGroup(groupID, true).addProposal(type, getCompilationUnit(), 10);
	}

	public void addLinkedPositionProposal(String groupID, String proposal) {
		ProposalCore p = new ProposalCore(proposal, 10);
		getLinkedProposalModel().getPositionGroup(groupID, true).addProposal(p);
	}

}