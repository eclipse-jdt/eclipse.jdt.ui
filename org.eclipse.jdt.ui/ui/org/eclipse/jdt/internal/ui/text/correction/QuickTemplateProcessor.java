/**********************************************************************
Copyright (c) 2000, 2003 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
	IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.ContextTypeRegistry;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.JavaCodeReader;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.text.template.TemplateEngine;
import org.eclipse.jdt.internal.ui.text.template.TemplateProposal;


/**
 * Quick template processor.
 */
public class QuickTemplateProcessor implements IContentAssistProcessor {
		
	private IEditorPart fEditor;
	private IWorkingCopyManager fManager;
	private JavaCompletionProposalComparator fComparator;
	private TemplateEngine fTemplateEngine;
	
	
	public QuickTemplateProcessor(IEditorPart editor) {
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		ContextType contextType= ContextTypeRegistry.getInstance().getContextType(JavaContextType.NAME);
		if (contextType != null)
			fTemplateEngine= new TemplateEngine(contextType);
		
		fComparator= new JavaCompletionProposalComparator();
	}
	
	private boolean looksLikeMethod(JavaCodeReader reader) throws IOException {
		int curr= reader.read();
		while (curr != JavaCodeReader.EOF && Character.isWhitespace((char) curr))
			curr= reader.read();
			
		if (curr == JavaCodeReader.EOF)
			return false;

		return Character.isJavaIdentifierPart((char) curr) || Character.isJavaIdentifierStart((char) curr);
	}
	
	private int guessContextInformationPosition(ITextViewer viewer, int offset) {
		int contextPosition= offset;
			
		IDocument document= viewer.getDocument();
		
		try {

			JavaCodeReader reader= new JavaCodeReader();
			reader.configureBackwardReader(document, offset, true, true);
	
			int nestingLevel= 0;

			int curr= reader.read();		
			while (curr != JavaCodeReader.EOF) {

				if (')' == (char) curr)
					++ nestingLevel;

				else if ('(' == (char) curr) {
					-- nestingLevel;
				
					if (nestingLevel < 0) {
						int start= reader.getOffset();
						if (looksLikeMethod(reader))
							return start + 1;
					}	
				}

				curr= reader.read();					
			}
		} catch (IOException e) {
		}
		
		return contextPosition;
	}		
	
	/**
	 * Order the given proposals.
	 */
	private ICompletionProposal[] order(ICompletionProposal[] proposals) {
		Arrays.sort(proposals, fComparator);
		return proposals;	
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		int contextInformationPosition= guessContextInformationPosition(viewer, documentOffset);
		return internalComputeCompletionProposals(viewer, documentOffset, contextInformationPosition);
	}
	
	private ICompletionProposal[] internalComputeCompletionProposals(ITextViewer viewer, int offset, int contextOffset) {
		
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());
		TemplateProposal[] templateResults= null;
		if (fTemplateEngine != null) {
			try {
				fTemplateEngine.reset();
				fTemplateEngine.complete(viewer, offset, unit);
			} catch (JavaModelException ex) {
				Shell shell= viewer.getTextWidget().getShell();
				ErrorDialog.openError(shell, CorrectionMessages.getString("QuickTemplateProcessor.error.accessing.title"), CorrectionMessages.getString("QuickTemplateProcessor.error.accessing.message"), ex.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
			}				
			
			templateResults= fTemplateEngine.getResults();
			
		}
		
		/*
		 * Order here and not in result collector to make sure that the order
		 * applies to all proposals and not just those of the compilation unit. 
		 */
		return order(templateResults);
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
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}
}