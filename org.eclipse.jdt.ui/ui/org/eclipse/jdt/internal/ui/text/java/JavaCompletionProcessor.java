/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.template.java.JavaContextType;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.text.JavaCodeReader;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateEngine;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal;


/**
 * Java completion processor.
 */
public class JavaCompletionProcessor implements IContentAssistProcessor {

	private static class ContextInformationWrapper implements IContextInformation, IContextInformationExtension {

		private final IContextInformation fContextInformation;
		private int fPosition;

		public ContextInformationWrapper(IContextInformation contextInformation) {
			fContextInformation= contextInformation;
		}

		/*
		 * @see IContextInformation#getContextDisplayString()
		 */
		public String getContextDisplayString() {
			return fContextInformation.getContextDisplayString();
		}

			/*
		 * @see IContextInformation#getImage()
		 */
		public Image getImage() {
			return fContextInformation.getImage();
		}

		/*
		 * @see IContextInformation#getInformationDisplayString()
		 */
		public String getInformationDisplayString() {
			return fContextInformation.getInformationDisplayString();
		}

		/*
		 * @see IContextInformationExtension#getContextInformationPosition()
		 */
		public int getContextInformationPosition() {
			return fPosition;
		}

		public void setContextInformationPosition(int position) {
			fPosition= position;
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.IContextInformation#equals(java.lang.Object)
		 */
		public boolean equals(Object object) {
			if (object instanceof ContextInformationWrapper)
				return fContextInformation.equals(((ContextInformationWrapper) object).fContextInformation);
			else
				return fContextInformation.equals(object);
		}
	}


	private final static String VISIBILITY= JavaCore.CODEASSIST_VISIBILITY_CHECK;
	private final static String ENABLED= "enabled"; //$NON-NLS-1$
	private final static String DISABLED= "disabled"; //$NON-NLS-1$


	protected IWorkingCopyManager fManager;
	private IEditorPart fEditor;
	private IContextInformationValidator fValidator;

	private char[] fProposalAutoActivationSet;
	private CompletionProposalComparator fComparator;

	private TemplateEngine fTemplateEngine;

	private int fNumberOfComputedResults= 0;
	private String fErrorMsg;


	public JavaCompletionProcessor(IEditorPart editor) {
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		TemplateContextType contextType= JavaPlugin.getDefault().getTemplateContextRegistry().getContextType("java"); //$NON-NLS-1$
		if (contextType == null) {
			contextType= new JavaContextType();
			JavaPlugin.getDefault().getTemplateContextRegistry().addContextType(contextType);
		}
		if (contextType != null)
			fTemplateEngine= new TemplateEngine(contextType);

		fComparator= new CompletionProposalComparator();
	}

	/**
	 * Sets this processor's set of characters triggering the activation of the
	 * completion proposal computation.
	 *
	 * @param activationSet the activation set
	 */
	public void setCompletionProposalAutoActivationCharacters(char[] activationSet) {
		fProposalAutoActivationSet= activationSet;
	}

	/**
	 * Tells this processor to restrict its proposal to those element
	 * visible in the actual invocation context.
	 *
	 * @param restrict <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToVisibility(boolean restrict) {
		Hashtable options= JavaCore.getOptions();
		Object value= options.get(VISIBILITY);
		if (value instanceof String) {
			String newValue= restrict ? ENABLED : DISABLED;
			if ( !newValue.equals(value)) {
				options.put(VISIBILITY, newValue);
				JavaCore.setOptions(options);
			}
		}
	}

	/**
	 * Tells this processor to order the proposals alphabetically.
	 *
	 * @param order <code>true</code> if proposals should be ordered.
	 */
	public void orderProposalsAlphabetically(boolean order) {
		fComparator.setOrderAlphabetically(order);
	}

	/**
	 * Tells this processor to restrict is proposals to those
	 * starting with matching cases.
	 *
	 * @param restrict <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToMatchingCases(boolean restrict) {
		// not yet supported
	}

	/**
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {

		if (fNumberOfComputedResults == 0) {
			if (fErrorMsg == null || fErrorMsg.trim().length() == 0)
				return JavaUIMessages.JavaEditor_codeassist_noCompletions;
		}
		return fErrorMsg;
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
		return fProposalAutoActivationSet;
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

	private List addContextInformations(ITextViewer viewer, int offset) {
		ICompletionProposal[] proposals= internalComputeCompletionProposals(viewer, offset);

		List result= new ArrayList();
		for (int i= 0; i < proposals.length; i++) {
			IContextInformation contextInformation= proposals[i].getContextInformation();
			if (contextInformation != null) {
				ContextInformationWrapper wrapper= new ContextInformationWrapper(contextInformation);
				wrapper.setContextInformationPosition(offset);
				result.add(wrapper);
			}
		}
		return result;
	}

	/**
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		int contextInformationPosition= guessContextInformationPosition(viewer, offset);
		List result= addContextInformations(viewer, contextInformationPosition);
		return (IContextInformation[]) result.toArray(new IContextInformation[result.size()]);
	}

	/**
	 * Order the given proposals.
	 */
	private ICompletionProposal[] order(ICompletionProposal[] proposals) {
		Arrays.sort(proposals, fComparator);
		return proposals;
	}

	/**
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		return internalComputeCompletionProposals(viewer, offset);
	}

	private ICompletionProposal[] internalComputeCompletionProposals(ITextViewer viewer, int offset) {

		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());
		if (unit == null)
			return new ICompletionProposal[0];
		
		ICompletionProposal[] results;

		CompletionProposalCollector collector;
		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES)) {
			collector= new ExperimentalResultCollector(unit);
		} else {
			collector= new CompletionProposalCollector(unit);
		}

		try {
			Point selection= viewer.getSelectedRange();
			if (selection.y > 0)
				collector.setReplacementLength(selection.y);
			
				unit.codeComplete(offset, collector);
		} catch (JavaModelException x) {
			Shell shell= viewer.getTextWidget().getShell();
			if (x.isDoesNotExist() && !unit.getJavaProject().isOnClasspath(unit))
				MessageDialog.openInformation(shell, JavaTextMessages.CompletionProcessor_error_notOnBuildPath_title, JavaTextMessages.CompletionProcessor_error_notOnBuildPath_message);
			else
				ErrorDialog.openError(shell, JavaTextMessages.CompletionProcessor_error_accessing_title, JavaTextMessages.CompletionProcessor_error_accessing_message, x.getStatus());
		}

		results= collector.getJavaCompletionProposals();
		int contextInformationOffset= guessContextInformationPosition(viewer, offset);
		if (contextInformationOffset != offset) {
			for (int i= 0; i < results.length; i++) {
				if (results[i] instanceof JavaMethodCompletionProposal) {
					JavaMethodCompletionProposal jmcp= (JavaMethodCompletionProposal) results[i];
					jmcp.setContextInformationPosition(contextInformationOffset);
				}
			}
		}
		fErrorMsg= collector.getErrorMessage();

		if (fTemplateEngine != null) {
			fTemplateEngine.reset();
			fTemplateEngine.complete(viewer, offset, unit);

			TemplateProposal[] templateResults= fTemplateEngine.getResults();

			// update relevance of template proposals that match with a keyword
			// give those templates slightly more relevance than the keyword to
			// sort them first
			IJavaCompletionProposal[] keyWordResults= collector.getKeywordCompletionProposals();
			for (int i= 0; i < keyWordResults.length; i++) {
				String keyword= keyWordResults[i].getDisplayString();
				for (int k= 0; k < templateResults.length; k++) {
					TemplateProposal curr= templateResults[k];
					if (curr.getTemplate().getName().startsWith(keyword)) {
						curr.setRelevance(keyWordResults[i].getRelevance() + 1);
					}
				}
			}

			// concatenate arrays
			ICompletionProposal[] total= new ICompletionProposal[results.length + templateResults.length];
			System.arraycopy(templateResults, 0, total, 0, templateResults.length);
			System.arraycopy(results, 0, total, templateResults.length, results.length);
			results= total;
		}

		fNumberOfComputedResults= (results == null ? 0 : results.length);

		/*
		 * Order here and not in result collector to make sure that the order
		 * applies to all proposals and not just those of the compilation unit.
		 */
		return order(results);
	}
}
