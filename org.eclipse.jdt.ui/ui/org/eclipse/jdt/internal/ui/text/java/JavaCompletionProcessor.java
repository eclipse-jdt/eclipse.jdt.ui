package org.eclipse.jdt.internal.ui.text.java;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */


import java.util.ResourceBundle;
import java.util.Vector;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Java completion processor.
 */
public class JavaCompletionProcessor implements IContentAssistProcessor {
	
	private final static String ERROR_TITLE= "Editor.error.access.title";
	private final static String ERROR_MESSAGE= "Editor.error.access.message";
	
	private IEditorPart fEditor;
	private ResultCollector fCollector;
	private IWorkingCopyManager fManager;
	
	
	public JavaCompletionProcessor(IEditorPart editor) {
		fEditor= editor;
		fCollector= new ResultCollector();
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
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
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());
		
		try {
			if (unit != null) {
				fCollector.reset(unit.getJavaProject());
				unit.codeComplete(offset, fCollector);
			}
		} catch (JavaModelException x) {
			ResourceBundle b= JavaPlugin.getDefault().getResourceBundle();
			Shell shell= viewer.getTextWidget().getShell();
			ErrorDialog.openError(shell, b.getString(ERROR_TITLE), b.getString(ERROR_MESSAGE), x.getStatus());
		}
		
		ICompletionProposal[] results= fCollector.getResults();
		return results;
	}
}