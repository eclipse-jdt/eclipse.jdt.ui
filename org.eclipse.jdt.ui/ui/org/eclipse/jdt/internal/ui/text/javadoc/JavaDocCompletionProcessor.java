/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.javadoc;


import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.ContextType;
import org.eclipse.jface.text.templates.ContextTypeRegistry;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavadocCompletionProcessor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateEngine;

/**
 * Java doc completion processor using contributed IJavaDocCompletionProcessor's
 * to evaluate proposals.
 */
public class JavaDocCompletionProcessor implements IContentAssistProcessor {
	
	private static final String PROCESSOR_CONTRIBUTION_ID= "javadocCompletionProcessor"; //$NON-NLS-1$
	
	private IEditorPart fEditor;
	private char[] fProposalAutoActivationSet;
	private JavaCompletionProposalComparator fComparator;
	private TemplateEngine fTemplateEngine;
	private int fSubProcessorFlags;
	private String fErrorMessage;
	
	private IJavadocCompletionProcessor[] fSubProcessors;
	
	public JavaDocCompletionProcessor(IEditorPart editor) {
		fEditor= editor;
		ContextType contextType= ContextTypeRegistry.getInstance().getContextType("javadoc"); //$NON-NLS-1$
		if (contextType != null)
			fTemplateEngine= new TemplateEngine(contextType);
		fSubProcessorFlags= 0;
		fComparator= new JavaCompletionProposalComparator();
		fSubProcessors= null;
		fErrorMessage= null;
	}


	private IJavadocCompletionProcessor[] getContributedProcessors() {
		if (fSubProcessors == null) {
			try {
				IPluginRegistry registry= Platform.getPluginRegistry();
				IConfigurationElement[] elements=	registry.getConfigurationElementsFor(JavaUI.ID_PLUGIN, PROCESSOR_CONTRIBUTION_ID);
				IJavadocCompletionProcessor[] result= new IJavadocCompletionProcessor[elements.length];
				for (int i= 0; i < elements.length; i++) {
					result[i]= (IJavadocCompletionProcessor) elements[i].createExecutableExtension("class"); //$NON-NLS-1$
				}
				fSubProcessors= result;
			} catch (CoreException e) {
				JavaPlugin.log(e);
				fSubProcessors= new IJavadocCompletionProcessor[] { new JavaDocCompletionEvaluator() };
			}
		}
		return fSubProcessors;
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
		fSubProcessorFlags= restrict ? IJavadocCompletionProcessor.RESTRICT_TO_MATCHING_CASE : 0;
	}
	
	/**
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
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
		return fProposalAutoActivationSet;
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
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		ICompilationUnit cu= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());

		ArrayList result= new ArrayList();
		String errorMessage= null;

		IJavadocCompletionProcessor[] processors= getContributedProcessors();
		for (int i= 0; i < processors.length; i++) {
			IJavadocCompletionProcessor curr= processors[i];
			IContextInformation[] contextInfos= curr.computeContextInformation(cu, offset);
			if (contextInfos != null) {
				for (int k= 0; k < contextInfos.length; k++) {
					result.add(contextInfos[k]);
				}
			}
			if (curr.getErrorMessage() != null) {
				errorMessage= curr.getErrorMessage();
			}
		}
		fErrorMessage= result.isEmpty() ? errorMessage : null;
		return (IContextInformation[]) result.toArray(new IContextInformation[result.size()]);
	}

	/**
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		ICompilationUnit cu= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
	
		int offset= documentOffset;
		int length= 0;	
		Point selection= viewer.getSelectedRange();
		if (selection.y > 0) {
			offset= selection.x;
			length= selection.y;
		}

		ArrayList result= new ArrayList();
		String errorMessage= null;

		IJavadocCompletionProcessor[] processors= getContributedProcessors();
		for (int i= 0; i < processors.length; i++) {
			IJavadocCompletionProcessor curr= processors[i];
			IJavaCompletionProposal[] proposals= curr.computeCompletionProposals(cu, offset, length, fSubProcessorFlags);
			if (proposals != null) {
				for (int k= 0; k < proposals.length; k++) {
					result.add(proposals[k]);
				}
			}
			if (curr.getErrorMessage() != null) {
				errorMessage= curr.getErrorMessage();
			}
		}
		if (fTemplateEngine != null) {
			try {
				fTemplateEngine.reset(); 
				fTemplateEngine.complete(viewer, offset, cu);
			} catch (JavaModelException x) {
				errorMessage= x.getLocalizedMessage();
			}				
			
			ICompletionProposal[] templateResults= fTemplateEngine.getResults();
			for (int k= 0; k < templateResults.length; k++) {
				result.add(templateResults[k]);
			}
		}
		fErrorMessage= result.isEmpty() ? errorMessage : null;
		
		IJavaCompletionProposal[] total= (IJavaCompletionProposal[]) result.toArray(new IJavaCompletionProposal[result.size()]);	
		return order(total);			
	}
	
	/**
	 * Order the given proposals.
	 */
	private IJavaCompletionProposal[] order(IJavaCompletionProposal[] proposals) {
		Arrays.sort(proposals, fComparator);
		return proposals;	
	}
}
