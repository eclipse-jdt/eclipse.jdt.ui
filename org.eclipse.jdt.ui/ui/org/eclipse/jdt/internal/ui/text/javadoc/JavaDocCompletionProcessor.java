package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;



/**
 * Simple Java doc completion processor.
 */
public class JavaDocCompletionProcessor implements IContentAssistProcessor {
	
	private IEditorPart fEditor;
	private IWorkingCopyManager fManager;

	public JavaDocCompletionProcessor(IEditorPart editor) {
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
	}
	

	/**
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		try {
			ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());
			if (unit != null) {
				IDocument document= viewer.getDocument();
				
				int offset= documentOffset;
				int length= 0;
				
				Point selection= viewer.getSelectedRange();
				if (selection.y > 0) {
					offset= selection.x;
					length= selection.y;
				}
				
				CompletionEvaluator evaluator= new CompletionEvaluator(unit, document, offset, length);
				return evaluator.computeProposals();
			}
		} catch (JavaModelException x) {
		}
		
		return new ICompletionProposal[0];
	}
}