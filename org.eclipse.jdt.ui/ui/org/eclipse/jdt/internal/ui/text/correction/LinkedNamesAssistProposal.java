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


import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.link.LinkedEnvironment;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionGroup;
import org.eclipse.jdt.internal.ui.text.link.LinkedUIControl;

/**
 * A template proposal.
 */
public class LinkedNamesAssistProposal implements IJavaCompletionProposal, ICompletionProposalExtension2 {

	private SimpleName fNode;
	private IRegion fSelectedRegion; // initialized by apply()
	private ICompilationUnit fCompilationUnit;
			
	public LinkedNamesAssistProposal(ICompilationUnit cu, SimpleName node) {
		fNode= node;
		fCompilationUnit= cu;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		try {
			// create full ast
			CompilationUnit root= AST.parseCompilationUnit(fCompilationUnit, true);
			SimpleName nameNode= (SimpleName) NodeFinder.perform(root, fNode.getStartPosition(), fNode.getLength());

			ASTNode[] sameNodes= LinkedNodeFinder.perform(root, nameNode.resolveBinding());
			IDocument document= viewer.getDocument();
			
			LinkedPositionGroup group= new LinkedPositionGroup();
			for (int i= 0; i < sameNodes.length; i++) {
				ASTNode elem= sameNodes[i];
				group.createPosition(document, elem.getStartPosition(), elem.getLength(), i); // let the user iterate over all the linked fields
			}
			
			LinkedEnvironment enviroment= LinkedEnvironment.createLinkedEnvironment(document);
			enviroment.addGroup(group);
			
			LinkedUIControl ui= new LinkedUIControl(enviroment, viewer);
//			ui.setInitialOffset(offset);
			ui.setExitPosition(viewer, offset, 0, false);
			ui.enter();
			
			fSelectedRegion= ui.getSelectedRegion();
		} catch (BadLocationException e) {
		}
	}	

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		// can't do anything
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return CorrectionMessages.getString("LinkedNamesAssistProposal.proposalinfo"); //$NON-NLS-1$
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return CorrectionMessages.getString("LinkedNamesAssistProposal.description"); //$NON-NLS-1$
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

	/*
	 * @see IJavaCompletionProposal#getRelevance()
	 */
	public int getRelevance() {
		return 1;
	}
		
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer, boolean)
	 */
	public void selected(ITextViewer textViewer, boolean smartToggle) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(org.eclipse.jface.text.ITextViewer)
	 */
	public void unselected(ITextViewer textViewer) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 */
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return false;
	}

}
