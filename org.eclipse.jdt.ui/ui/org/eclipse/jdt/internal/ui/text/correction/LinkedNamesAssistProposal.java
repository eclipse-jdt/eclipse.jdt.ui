package org.eclipse.jdt.internal.ui.text.correction;


import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;

/**
 * A template proposal.
 */
public class LinkedNamesAssistProposal implements IJavaCompletionProposal, ICompletionProposalExtension2 {

	private SimpleName fNode;
	private IRegion fSelectedRegion; // initialized by apply()
			
	private static class LinkedNodeFinder extends ASTVisitor {
		private IBinding fBinding;
		private ArrayList fResult;
		
		public LinkedNodeFinder(IBinding binding, ArrayList result) {
			fBinding= binding;
			fResult= result;
		}
		
		public boolean visit(MethodDeclaration node) {
			if (node.isConstructor() && fBinding.getKind() == IBinding.TYPE) {
				ASTNode typeNode= node.getParent();
				if (typeNode instanceof TypeDeclaration) {
					if (fBinding == ((TypeDeclaration) typeNode).resolveBinding()) {
						fResult.add(node.getName());
					}
				}
			}
			return true;
		}
		
		public boolean visit(SimpleName node) {
			if (fBinding == node.resolveBinding()) {
				fResult.add(node);
			}
			return false;
		}

	}


	public LinkedNamesAssistProposal(SimpleName node) {
		fNode= node;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		try {
			ArrayList sameNodes= new ArrayList();
			LinkedNodeFinder finder= new LinkedNodeFinder(fNode.resolveBinding(), sameNodes);
			ASTResolving.findParentCompilationUnit(fNode).accept(finder);
			
			IDocument document= viewer.getDocument();
			LinkedPositionManager manager= new LinkedPositionManager(document);
			
			for (int i= 0; i < sameNodes.size(); i++) {
				ASTNode elem= (ASTNode) sameNodes.get(i);
				manager.addPosition(elem.getStartPosition(), elem.getLength());
			}
			
			LinkedPositionUI editor= new LinkedPositionUI(viewer, manager);
			editor.setInitialOffset(offset);
			editor.setFinalCaretOffset(offset);
			editor.enter();
			
			fSelectedRegion= editor.getSelectedRegion();
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