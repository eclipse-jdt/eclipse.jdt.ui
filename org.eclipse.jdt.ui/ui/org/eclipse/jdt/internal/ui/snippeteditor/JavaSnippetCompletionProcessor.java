/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;

import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.ResultCollector;


/**
 * Java snippet completion processor.
 */
public class JavaSnippetCompletionProcessor implements IContentAssistProcessor {
	
	private final static String ERROR_TITLE= "Editor.Error.access.title";
	private final static String ERROR_MESSAGE= "Editor.Error.access.message";
	
	private ResultCollector fCollector;
	private JavaSnippetEditor fEditor;
	
	public JavaSnippetCompletionProcessor(JavaSnippetEditor editor) {
		fCollector= new ResultCollector();
		fEditor= editor;
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
	 * @see IContentAssistProcessor#computeProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int position) {
		try {
			fCollector.reset(fEditor.findJavaProject());
			fEditor.codeComplete(fCollector);
		} catch (JavaModelException x) {
			ResourceBundle b= JavaPlugin.getResourceBundle();
			Shell shell= viewer.getTextWidget().getShell();
			ErrorDialog.openError(shell, b.getString(ERROR_TITLE), b.getString(ERROR_MESSAGE), x.getStatus());
		}
		return fCollector.getResults();
	}
}
