/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.text.java.JavaParameterListValidator;
import org.eclipse.jdt.internal.ui.text.java.ResultCollector;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.internal.ui.text.template.TemplateContext;
import org.eclipse.jdt.internal.ui.text.template.TemplateEngine;

/**
 * Java snippet completion processor.
 */
public class JavaSnippetCompletionProcessor implements IContentAssistProcessor {
	
	private ResultCollector fCollector;
	private JavaSnippetEditor fEditor;
	private IContextInformationValidator fValidator;
	private TemplateEngine fTemplateEngine;
	
	public JavaSnippetCompletionProcessor(JavaSnippetEditor editor) {
		fCollector= new ResultCollector();
		fEditor= editor;
		fTemplateEngine= new TemplateEngine(TemplateContext.JAVA);
	}
	
	/**
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fCollector.getErrorMessage();
	}

	/**
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		if (fValidator == null) {
			fValidator= new JavaParameterListValidator();
		}
		return fValidator;
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
	 * @see IContentAssistProcessor#computeProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int position) {
		try {
			fCollector.reset(fEditor.findJavaProject(), null);
			fEditor.codeComplete(fCollector);
		} catch (JavaModelException x) {
			Shell shell= viewer.getTextWidget().getShell();
			ErrorDialog.openError(shell, SnippetMessages.getString("CompletionProcessor.errorTitle"), SnippetMessages.getString("CompletionProcessor.errorMessage"), x.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
		}
		
		ICompletionProposal[] results= fCollector.getResults();
		
		try {
			fTemplateEngine.reset();
			fTemplateEngine.complete(viewer, position, null);
		} catch (JavaModelException x) {
			Shell shell= viewer.getTextWidget().getShell();
			ErrorDialog.openError(shell, SnippetMessages.getString("CompletionProcessor.errorTitle"), SnippetMessages.getString("CompletionProcessor.errorMessage"), x.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
		}				
		
		ICompletionProposal[] templateResults= fTemplateEngine.getResults();

		// concatenate arrays
		ICompletionProposal[] total= new ICompletionProposal[results.length + templateResults.length];
		System.arraycopy(templateResults, 0, total, 0, templateResults.length);
		System.arraycopy(results, 0, total, templateResults.length, results.length);
		
		return total;
	}
}
