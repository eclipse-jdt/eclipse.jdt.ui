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

package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.CompletionRequestorAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension;
import org.eclipse.jface.text.contentassist.IContentAssistSubject;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.text.java.JavaTypeCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.ProposalInfo;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

import org.eclipse.jdt.ui.PreferenceConstants;


public class JavaTypeCompletionProcessor implements IContentAssistProcessor, IContentAssistProcessorExtension {
	
	private static final String CLASS_NAME= "$$__$$";
	private static final String CU_NAME= CLASS_NAME + ".java";
	private static final String CU_START= "public class " + CLASS_NAME + " { ";
	private static final String CU_END= " }";
	
	private IPackageFragmentRoot fRoot;
	private JavaCompletionProposalComparator fComparator;
	private String fErrorMessage;
	private char[] fProposalAutoActivationSet;
	
	public JavaTypeCompletionProcessor(IPackageFragmentRoot root) {
		fRoot= root;
		fComparator= new JavaCompletionProposalComparator();
		
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getJavaTextTools().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet = triggers.toCharArray();
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported");
		return null;
	}
	
	/*
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported");
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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension#computeContextInformation(org.eclipse.jface.text.contentassist.IContentAssistSubject, int)
	 */
	public IContextInformation[] computeContextInformation(IContentAssistSubject contentAssistSubject, int documentOffset) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension#computeCompletionProposals(org.eclipse.jface.text.contentassist.IContentAssistSubject, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubject contentAssistSubject, int documentOffset) {
		String input= "";
		try {
			input= contentAssistSubject.getDocument().get(0, documentOffset);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		if (input.length() == 0)
			return null;
		
		String cuString= CU_START + input + CU_END;
		IPackageFragment defaultPackage= fRoot.getPackageFragment("");
		ICompilationUnit cu= defaultPackage.getCompilationUnit(CU_NAME);
		try {
			/*
			 * Explicitly create a new non-shared working copy.
			 */
			cu= (ICompilationUnit) cu.getWorkingCopy();
			cu.getBuffer().setContents(cuString);
			int cuOffset= CU_START.length() + input.length();
			TypeCompletionCollector collector= new TypeCompletionCollector(cuOffset - documentOffset, fRoot.getJavaProject());
			cu.codeComplete(cuOffset, collector);
			
			JavaCompletionProposal[] proposals= collector.getResults();
			if (proposals.length == 0) {
				String errorMsg= collector.getErrorMessage();
				if (errorMsg == null || errorMsg.trim().length() == 0)
					errorMsg= JavaUIMessages.getString("JavaEditor.codeassist.noCompletions"); //$NON-NLS-1$
				fErrorMessage= errorMsg;
			} else {
				fErrorMessage= collector.getErrorMessage();
			}
			Arrays.sort(proposals, fComparator);
			return proposals;
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		} finally {
			cu.destroy();
		}
	}

	private static class TypeCompletionCollector extends CompletionRequestorAdapter {
		private final ImageDescriptorRegistry fRegistry= JavaPlugin.getImageDescriptorRegistry();
		private List fProposals = new ArrayList();
		private int fOffsetReduction;
		private IJavaProject fJavaProject;
		private String fErrorMessage2;
		
		public TypeCompletionCollector(int offsetReduction, IJavaProject javaProject) {
			fOffsetReduction= offsetReduction;
			fJavaProject= javaProject;
		}

		public void acceptClass(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end, int relevance) {
			if (isDummyClass(typeName))
				return;
			ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(false, false, modifiers);
			ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
			fProposals.add(createTypeCompletion(start, end, new String(completionName), descriptor, new String(typeName), new String(packageName), info, relevance));
		}
		
		public void acceptError(IProblem error) {
			fErrorMessage2= error.getMessage();
		}
		
		public void acceptInterface(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end, int relevance) {
			if (isDummyClass(typeName))
				return;
			ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(true, false, modifiers);
			ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
			fProposals.add(createTypeCompletion(start, end, new String(completionName), descriptor, new String(typeName), new String(packageName), info, relevance));
		}
		
		public void acceptType(char[] packageName, char[] typeName, char[] completionName, int start, int end, int relevance) {
			if (isDummyClass(typeName))
				return;
			ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
			fProposals.add(createTypeCompletion(start, end, new String(completionName), JavaPluginImages.DESC_OBJS_CLASS, new String(typeName), new String(packageName), info, relevance));
		}
		
		public void acceptPackage(char[] packageName, char[] completionName, int start, int end, int relevance) {
			fProposals.add(createCompletion(start, end, new String(completionName), JavaPluginImages.DESC_OBJS_PACKAGE, new String(packageName), relevance));
		}
		
		private static boolean isDummyClass(char[] typeName) {
			return new String(typeName).equals(CLASS_NAME);
		}

		public JavaCompletionProposal[] getResults() {
			return (JavaCompletionProposal[]) fProposals.toArray(new JavaCompletionProposal[fProposals.size()]);
		}
		
		public String getErrorMessage() {
			return fErrorMessage2;
		}
		
		protected JavaCompletionProposal createCompletion(int start, int end, String completion, ImageDescriptor descriptor, String name, int relevance) {
			return new JavaCompletionProposal(completion, start - fOffsetReduction, end-start, getImage(descriptor), name, relevance);
		}
		
		protected JavaCompletionProposal createTypeCompletion(int start, int end, String completion, ImageDescriptor descriptor, String typeName, String containerName, ProposalInfo proposalInfo, int relevance) {
			String fullName= JavaModelUtil.concatenateName(containerName, typeName); // containername can be null
			
			StringBuffer buf= new StringBuffer(Signature.getSimpleName(fullName));
			String typeQualifier= Signature.getQualifier(fullName);
			buf.append(" - "); //$NON-NLS-1$
			if (typeQualifier.length() > 0) {
				buf.append(typeQualifier);
			} else if (containerName != null) {
//TODO				buf.append(JavaTextMessages.getString("ResultCollector.default_package")); //$NON-NLS-1$
			}
			String name= buf.toString();
			
			JavaCompletionProposal proposal= new JavaTypeCompletionProposal(completion, null,
				start - fOffsetReduction, end-start,
				getImage(descriptor), name, relevance, typeName, containerName);
			proposal.setProposalInfo(proposalInfo);
			return proposal;
		}

		private Image getImage(ImageDescriptor descriptor) {
			return (descriptor == null) ? null : fRegistry.get(descriptor);
		}
	}
		
}
