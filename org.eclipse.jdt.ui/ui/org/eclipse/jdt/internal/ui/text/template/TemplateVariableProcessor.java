package org.eclipse.jdt.internal.ui.text.template;

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.TemplateMessages;
import org.eclipse.jdt.internal.corext.template.TemplateVariable;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TemplateVariableProcessor implements IContentAssistProcessor {	
	
	/** the context type */
	private ContextType fContextType;
	
	/**
	 * Sets the context type.
	 */
	public void setContextType(ContextType contextType) {
		fContextType= contextType;	
	}
	
	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,	int documentOffset) {

		if (fContextType == null)
			return null;

		Vector vector= new Vector();		

		try {
			int offset= (documentOffset > 0) && (viewer.getDocument().get(documentOffset - 1, 1).equals("$")) //$NON-NLS-1$
				? documentOffset - 1
				: documentOffset;		
			int length= documentOffset - offset;

			for (Iterator iterator= fContextType.variableIterator(); iterator.hasNext(); ) {
				TemplateVariable variable= (TemplateVariable) iterator.next();
				vector.add(new TemplateVariableProposal(variable, offset, length, viewer));
			}

		} catch (BadLocationException e) {
			JavaPlugin.log(e);

			Shell shell= viewer.getTextWidget().getShell();
			MessageDialog.openError(shell, TemplateMessages.getString("TemplateVariableProcessor.error.title"), e.getMessage()); //$NON-NLS-1$
		}
		
		return (ICompletionProposal[]) vector.toArray(new ICompletionProposal[vector.size()]);
	}

	/*
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] {'$'};
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}

