/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;

/**
 *
 */
public class LinkedCorrectionProposal extends ASTRewriteCorrectionProposal {

	private GroupDescription fSelectionDescription;
	private List fLinkedPositions;
	private Map fLinkProposals;

	public LinkedCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, rewrite, relevance, image);
		fSelectionDescription= null;
		fLinkedPositions= null;
		fLinkProposals= null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getSelectionDescription()
	 */
	protected GroupDescription getSelectionDescription() {
		return fSelectionDescription;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getLinkedRanges()
	 */
	protected GroupDescription[] getLinkedRanges() {
		if (fLinkedPositions != null && !fLinkedPositions.isEmpty()) {
			return (GroupDescription[]) fLinkedPositions.toArray(new GroupDescription[fLinkedPositions.size()]);
		}
		return null;
	}
	
	public GroupDescription markAsSelection(ASTRewrite rewrite, ASTNode node) {
		fSelectionDescription= new GroupDescription("selection"); //$NON-NLS-1$
		rewrite.markAsTracked(node, fSelectionDescription);
		return fSelectionDescription;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getLinkedModeProposals(java.lang.String)
	 */
	protected ICompletionProposal[] getLinkedModeProposals(String name) {
		if (fLinkProposals == null) {
			return null;
		}
		List proposals= (List) fLinkProposals.get(name);
		if (proposals != null) {
			return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
		}
		return null;
	}
	
	public void addLinkedModeProposal(String name, String proposal) {
		addLinkedModeProposal(name, new LinkedModeProposal(proposal));
	}
	
	public void addLinkedModeProposal(String name, ICompletionProposal proposal) {
		if (fLinkProposals == null) {
			fLinkProposals= new HashMap();
		}
		List proposals= (List) fLinkProposals.get(name);
		if (proposals == null) {
			proposals= new ArrayList(10);
			fLinkProposals.put(name, proposals);			
		}
		proposals.add(proposal);
	}
	
	public GroupDescription markAsLinked(ASTRewrite rewrite, ASTNode node, boolean isFirst, String kind) {
		GroupDescription description= new GroupDescription(kind);
		rewrite.markAsTracked(node, description);
		if (fLinkedPositions == null) {
			fLinkedPositions= new ArrayList();
		}
		if (isFirst) {
			fLinkedPositions.add(0, description);
		} else {
			fLinkedPositions.add(description);
		}
		return description;
	}
	
	public void setSelectionDescription(GroupDescription desc) {
		fSelectionDescription= desc;
	}	
}
