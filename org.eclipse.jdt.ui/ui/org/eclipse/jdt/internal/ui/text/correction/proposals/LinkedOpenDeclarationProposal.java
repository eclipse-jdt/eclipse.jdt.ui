/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation.
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
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.ui.IWorkbenchPage;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * Quick Navigation proposal.
 */
public class LinkedOpenDeclarationProposal implements IJavaCompletionProposal, ICompletionProposalExtension2, ICommandAccess {

	public static final String ASSIST_ID= "org.eclipse.jdt.ui.correction.showOriginalDeclaration.assist"; //$NON-NLS-1$
	private SimpleName fNode;
	private String fLabel;

	public LinkedOpenDeclarationProposal(String label, SimpleName node) {
		fLabel= label;
		fNode= node;
	}

	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		IWorkbenchPage page= JavaPlugin.getActiveWorkbenchWindow().getActivePage();
		if (page == null) {
			return;
		}
		if (page.getActiveEditor() instanceof ITextEditor textEditor) {
			final ASTNode[] result = new ASTNode[1];
			String varName = fNode.getFullyQualifiedName();
			ASTNode scopeOwner = fNode;
			boolean[] found= { false };
			while (scopeOwner != null && !found[0]) {

				scopeOwner.accept(new ASTVisitor() {
					@Override
					public boolean visit(VariableDeclarationFragment node) {
						if (!found[0] && node.getName().getIdentifier().equals(varName) && node.getStartPosition() != fNode.getStartPosition()) {
							result[0]= node;
							found[0]= true;
							return false;
						}
						return true;
					}

					@Override
					public boolean visit(SingleVariableDeclaration node) {
						if (!found[0] && node.getName().getIdentifier().equals(varName) && node.getStartPosition() != fNode.getStartPosition()) {
							result[0]= node;
							found[0]= true;
							return false;
						}
						return true;
					}
				});
				scopeOwner= scopeOwner.getParent();
			}
			if (scopeOwner == null) {
				return;
			}

			ASTNode decl= result[0];
			SimpleName name= ((VariableDeclaration) decl).getName();
			int start= name.getStartPosition();
			int length= name.getLength();
			textEditor.selectAndReveal(start, length);
		}
	}
	@Override
	public void apply(IDocument document) {
	}

	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return fLabel;
	}

	@Override
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_SHOW_DECLARATION);
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}


	@Override
	public void selected(ITextViewer textViewer, boolean smartToggle) {
	}

	@Override
	public void unselected(ITextViewer textViewer) {
	}

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return false;
	}

	@Override
	public String getCommandId() {
		return ASSIST_ID;
	}

	@Override
	public int getRelevance() {
		return 0;
	}

}
