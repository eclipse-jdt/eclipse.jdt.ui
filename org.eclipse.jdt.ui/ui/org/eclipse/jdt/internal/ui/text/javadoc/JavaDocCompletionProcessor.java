package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.template.TemplateEngine;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;



/**
 * Simple Java doc completion processor.
 */
public class JavaDocCompletionProcessor implements IContentAssistProcessor {
	
	private IEditorPart fEditor;
	private IWorkingCopyManager fManager;
	private TemplateEngine fTemplateEngine;


	public JavaDocCompletionProcessor(IEditorPart editor) {
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		fTemplateEngine= new TemplateEngine(TemplateEngine.JAVADOC); //$NON-NLS-1$
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
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());
		IDocument document= viewer.getDocument();

		ICompletionProposal[] results= new ICompletionProposal[0];

		try {
			if (unit != null) {
				
				int offset= documentOffset;
				int length= 0;
				
				Point selection= viewer.getSelectedRange();
				if (selection.y > 0) {
					offset= selection.x;
					length= selection.y;
				}
				
				CompletionEvaluator evaluator= new CompletionEvaluator(unit, document, offset, length);
				results= evaluator.computeProposals();
			}
		} catch (JavaModelException x) {
		}

		try {
			if (unit != null) {			
				fTemplateEngine.reset(viewer);
				fTemplateEngine.complete(unit, documentOffset);
			}			
		} catch (JavaModelException x) {
		}				
		
		ICompletionProposal[] exactTemplateResults= fTemplateEngine.getExactResults();
		ICompletionProposal[] notExactTemplateResults= fTemplateEngine.getNotExactResults();

		// concatenate arrays
		ICompletionProposal[] total= new ICompletionProposal[results.length + exactTemplateResults.length + notExactTemplateResults.length];
		System.arraycopy(exactTemplateResults, 0, total, 0, exactTemplateResults.length);
		System.arraycopy(results, 0, total, exactTemplateResults.length, results.length);
		System.arraycopy(notExactTemplateResults, 0, total, exactTemplateResults.length + results.length, notExactTemplateResults.length);

		return total;
	}
}