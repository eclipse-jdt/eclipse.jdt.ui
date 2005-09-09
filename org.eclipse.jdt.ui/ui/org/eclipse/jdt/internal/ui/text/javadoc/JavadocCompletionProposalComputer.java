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
package org.eclipse.jdt.internal.ui.text.javadoc;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.contentassist.ICompletionProposalComputer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavadocCompletionProcessor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.JavaContentAssistInvocationContext;

/**
 * Java doc completion processor using contributed IJavaDocCompletionProcessor's
 * to evaluate proposals.
 */
public class JavadocCompletionProposalComputer implements ICompletionProposalComputer {

	private static final String PROCESSOR_CONTRIBUTION_ID= "javadocCompletionProcessor"; //$NON-NLS-1$

	private IJavadocCompletionProcessor[] fSubProcessors;

	public JavadocCompletionProposalComputer() {
		fSubProcessors= null;
	}


	private IJavadocCompletionProcessor[] getContributedProcessors() {
		if (fSubProcessors == null) {
			try {
				IExtensionRegistry registry= Platform.getExtensionRegistry();
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

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public List computeContextInformation(TextContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
			
			ICompilationUnit cu= javaContext.computeCompilationUnit();
			int offset= javaContext.getInvocationOffset();
			
			ArrayList result= new ArrayList();
			
			IJavadocCompletionProcessor[] processors= getContributedProcessors();
			for (int i= 0; i < processors.length; i++) {
				IJavadocCompletionProcessor curr= processors[i];
				IContextInformation[] contextInfos= curr.computeContextInformation(cu, offset);
				if (contextInfos != null) {
					for (int k= 0; k < contextInfos.length; k++) {
						result.add(contextInfos[k]);
					}
				}
			}
			return result;
		}
		return Collections.EMPTY_LIST;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeCompletionProposals(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public List computeCompletionProposals(TextContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (context instanceof JavadocContentAssistInvocationContext) {
			JavadocContentAssistInvocationContext javaContext= (JavadocContentAssistInvocationContext) context;
			
			ICompilationUnit cu= javaContext.computeCompilationUnit();
			int offset= javaContext.getInvocationOffset();
			int length= javaContext.getSelectionLength();
			Point selection= javaContext.getViewer().getSelectedRange();
			if (selection.y > 0) {
				offset= selection.x;
				length= selection.y;
			}
			
			ArrayList result= new ArrayList();
			
			IJavadocCompletionProcessor[] processors= getContributedProcessors();
			for (int i= 0; i < processors.length; i++) {
				IJavadocCompletionProcessor curr= processors[i];
				IJavaCompletionProposal[] proposals= curr.computeCompletionProposals(cu, offset, length, javaContext.getFlags());
				if (proposals != null) {
					for (int k= 0; k < proposals.length; k++) {
						result.add(proposals[k]);
					}
				}
			}
			return result;
		}
		return Collections.EMPTY_LIST;
	}
}
