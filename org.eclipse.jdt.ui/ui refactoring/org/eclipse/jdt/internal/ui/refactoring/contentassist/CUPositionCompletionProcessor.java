/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.CompletionRequestorAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.text.java.JavaTypeCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.ProposalInfo;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

import org.eclipse.jdt.ui.PreferenceConstants;


public class CUPositionCompletionProcessor implements IContentAssistProcessor, ISubjectControlContentAssistProcessor {
	
	private static final ImageDescriptorRegistry IMAGE_DESC_REGISTRY= JavaPlugin.getImageDescriptorRegistry();
	
	private String fErrorMessage;
	private char[] fProposalAutoActivationSet;
	private JavaCompletionProposalComparator fComparator;
	
	private ICompilationUnit fOriginalCu;
	private String fBeforeString;
	private String fAfterString;

	private CUPositionCompletionRequestor fCompletionRequestor;

	/**
	 * Creates a <code>CUPositionCompletionProcessor</code>.
	 * The completion context must be set via {@link #setCompletionContext(ICompilationUnit,String,String)}.
	 * @param completionRequestor the completion requestor
	 */
	public CUPositionCompletionProcessor(CUPositionCompletionRequestor completionRequestor) {
		fCompletionRequestor= completionRequestor;
		
		fComparator= new JavaCompletionProposalComparator();
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet = triggers.toCharArray();
	}

	/**
	 * @param cu the {@link ICompilationUnit} in whose context codeComplete will be invoked.
	 * 		The given cu doesn't have to exist (and if it exists, it will not be modified).
	 * 		An independent working copy consisting of
	 * 		<code>beforeString</code> + ${current_input} + <code>afterString</code> will be used.
	 * @param beforeString the string before the input position
	 * @param afterString the string after the input position
	 */
	public void setCompletionContext(ICompilationUnit cu, String beforeString, String afterString) {
		fOriginalCu= cu;
		fBeforeString= beforeString;
		fAfterString= afterString;
		if (cu != null)
			fCompletionRequestor.setJavaProject(cu.getJavaProject());
	}

	/**
	 * Computing proposals on a <code>ITextViewer</code> is not supported.
	 * @see #computeCompletionProposals(IContentAssistSubjectControl, int)
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}
	
	/**
	 * Computing context information on a <code>ITextViewer</code> is not supported.
	 * @see #computeContextInformation(IContentAssistSubjectControl, int)
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return fProposalAutoActivationSet;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null; //no context
	}

	/*
	 * @see ISubjectControlContentAssistProcessor#computeContextInformation(IContentAssistSubjectControl, int)
	 */
	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		return null;
	}

	/*
	 * @see ISubjectControlContentAssistProcessor#computeCompletionProposals(IContentAssistSubjectControl, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		if (fOriginalCu == null)
			return null;
		String input= contentAssistSubjectControl.getDocument().get();
		if (documentOffset == 0)
			return null;
		ICompletionProposal[] proposals= internalComputeCompletionProposals(documentOffset, input);
		Arrays.sort(proposals, fComparator);
		return proposals;
	}

	private ICompletionProposal[] internalComputeCompletionProposals(int documentOffset, String input) {
		String cuString= fBeforeString + input + fAfterString;
		ICompilationUnit cu= null;
		try {
			/*
			 * Explicitly create a new non-shared working copy.
			 * 
			 * The WorkingCopy cannot easily be shared between calls, since IContentAssistProcessor
			 * has no dispose() lifecycle method. A workaround could be to pass in a WorkingCopyOwner
			 * and dispose the owner's working copies from the caller's dispose().
			 */
			cu= fOriginalCu.getWorkingCopy(new WorkingCopyOwner() {/*subclass*/}, null, new NullProgressMonitor());
			cu.getBuffer().setContents(cuString);
			int cuPrefixLength= fBeforeString.length();
			fCompletionRequestor.setOffsetReduction(cuPrefixLength);
			cu.codeComplete(cuPrefixLength + documentOffset, fCompletionRequestor);
			
			JavaCompletionProposal[] proposals= fCompletionRequestor.getResults();
			if (proposals.length == 0) {
				String errorMsg= fCompletionRequestor.getErrorMessage();
				if (errorMsg == null || errorMsg.trim().length() == 0)
					errorMsg= RefactoringMessages.getString("JavaTypeCompletionProcessor.no_completion");  //$NON-NLS-1$
				fErrorMessage= errorMsg;
			} else {
				fErrorMessage= fCompletionRequestor.getErrorMessage();
			}
			return proposals;
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		} finally {
			try {
				cu.discardWorkingCopy();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
	}

	protected static class CUPositionCompletionRequestor extends CompletionRequestorAdapter {
		private int fOffsetReduction;
		private IJavaProject fJavaProject;
		
		private List fProposals;
		private String fErrorMessage2;

		public IJavaProject getJavaProject() {
			return fJavaProject;
		}
		
		private void setJavaProject(IJavaProject javaProject) {
			fJavaProject= javaProject;
		}
		
		private void setOffsetReduction(int offsetReduction) {
			fOffsetReduction= offsetReduction;
			fProposals= new ArrayList();
		}
		
		public final void acceptError(IProblem error) {
			fErrorMessage2= error.getMessage();
		}

		public final JavaCompletionProposal[] getResults() {
			return (JavaCompletionProposal[]) fProposals.toArray(new JavaCompletionProposal[fProposals.size()]);
		}
		
		public final String getErrorMessage() {
			return fErrorMessage2;
		}
		
		protected final void addAdjustedCompletion(String name, String completion,
				int start, int end, int relevance, ImageDescriptor descriptor) {
			fProposals.add(new JavaCompletionProposal(completion, start - fOffsetReduction, end - start,
					getImage(descriptor), name, relevance));
		}
		
		protected final void addAdjustedTypeCompletion(char[] packageName, char[] typeName, char[] completionName,
				int start, int end, int relevance, ImageDescriptor descriptor) {
			ProposalInfo proposalInfo= new ProposalInfo(getJavaProject(), packageName, typeName);
			fProposals.add(createTypeCompletion(new String(packageName), new String(typeName), proposalInfo,
					new String(completionName), start - fOffsetReduction, end - fOffsetReduction, relevance, descriptor));
		}

		private static JavaCompletionProposal createTypeCompletion(String containerName, String typeName, ProposalInfo proposalInfo,
				String completion, int start, int end, int relevance, ImageDescriptor descriptor) {
			String fullName= JavaModelUtil.concatenateName(containerName, typeName); // containername can be null
			
			StringBuffer buf= new StringBuffer(Signature.getSimpleName(fullName));
			String typeQualifier= Signature.getQualifier(fullName);
			buf.append(JavaElementLabels.CONCAT_STRING);
			if (typeQualifier.length() > 0) {
				buf.append(typeQualifier);
			} else if (containerName != null) {
				buf.append(JavaElementLabels.DEFAULT_PACKAGE);
			}
			String name= buf.toString();
			
			if ("java.lang".equals(containerName) && typeName.equals(completion)) //$NON-NLS-1$
				completion= containerName + '.' + completion; //since JDT core swallows java.lang
			
			JavaCompletionProposal proposal= new JavaTypeCompletionProposal(completion, /*insert fully qualified*/null,
					start, end-start, getImage(descriptor), name, relevance, typeName, containerName);
			proposal.setProposalInfo(proposalInfo);
			return proposal;
		}

		private static Image getImage(ImageDescriptor descriptor) {
			return (descriptor == null) ? null : CUPositionCompletionProcessor.IMAGE_DESC_REGISTRY.get(descriptor);
		}
	}
}
