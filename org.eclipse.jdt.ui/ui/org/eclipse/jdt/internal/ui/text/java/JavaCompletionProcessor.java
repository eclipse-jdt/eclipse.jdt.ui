package org.eclipse.jdt.internal.ui.text.java;

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
 * Java completion processor.
 */
public class JavaCompletionProcessor implements IContentAssistProcessor {
		
	private IEditorPart fEditor;
	private ResultCollector fCollector;
	private IWorkingCopyManager fManager;
	private IContextInformationValidator fValidator;
	private TemplateEngine fTemplateEngine;
	
	
	public JavaCompletionProcessor(IEditorPart editor) {
		fEditor= editor;
		fCollector= new ResultCollector();
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		fTemplateEngine= new TemplateEngine(TemplateEngine.JAVA); //$NON-NLS-1$
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
		if (fValidator == null)
			fValidator= new JavaParameterListValidator();
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
		return new char[] { '.', '(' };
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
		IDocument document= viewer.getDocument();
		
		try {
			if (unit != null) {
				
				fCollector.reset(unit.getJavaProject(), unit);
				Point selection= viewer.getSelectedRange();
				if (selection.y > 0)
					fCollector.setRegionToReplace(selection.x, selection.y);
				
				unit.codeComplete(offset, fCollector);
			}
		} catch (JavaModelException x) {
			Shell shell= viewer.getTextWidget().getShell();
			ErrorDialog.openError(shell, JavaTextMessages.getString("CompletionProcessor.error.accessing.title"), JavaTextMessages.getString("CompletionProcessor.error.accessing.message"), x.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
		}				

		ICompletionProposal[] results= fCollector.getResults();

		try {
			if (unit != null) {			
				fTemplateEngine.reset();
				fTemplateEngine.complete(unit, offset);
			}			
		} catch (JavaModelException x) {
			Shell shell= viewer.getTextWidget().getShell();
			ErrorDialog.openError(shell, JavaTextMessages.getString("CompletionProcessor.error.accessing.title"), JavaTextMessages.getString("CompletionProcessor.error.accessing.message"), x.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
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