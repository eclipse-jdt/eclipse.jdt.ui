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

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.CompletionRequestorAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
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
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.text.java.JavaTypeCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.ProposalInfo;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

import org.eclipse.jdt.ui.PreferenceConstants;


public class JavaTypeCompletionProcessor implements IContentAssistProcessor, ISubjectControlContentAssistProcessor {
	
	private static final ImageDescriptorRegistry IMAGE_DESC_REGISTRY= JavaPlugin.getImageDescriptorRegistry();
	private static final String CLASS_NAME= "$$__$$"; //$NON-NLS-1$
	private static final String CU_NAME= CLASS_NAME + ".java"; //$NON-NLS-1$
	private static final String CU_START= "public class " + CLASS_NAME + " { ";  //$NON-NLS-1$ //$NON-NLS-2$
	private static final String CU_END= " }"; //$NON-NLS-1$

	private static final String VOID= "void"; //$NON-NLS-1$
	private static final List BASE_TYPES= Arrays.asList(
		new String[] {"boolean", "byte", "char", "double", "float", "int", "long", "short"});  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	
	private IPackageFragment fPackageFragment;
	private boolean fEnableBaseTypes;
	private boolean fEnableVoid;
	private JavaCompletionProposalComparator fComparator;

	private String fErrorMessage;
	private char[] fProposalAutoActivationSet;
	
	/**
	 * Creates a <code>JavaTypeCompletionProcessor</code>.
	 * The completion context must be set via {@link #setPackageFragment(IPackageFragment)}.
	 * 
	 * @param enableBaseTypes complete java base types iff <code>true</code>
	 * @param enableVoid complete <code>void</code> base type iff <code>true</code>
	 */
	public JavaTypeCompletionProcessor(boolean enableBaseTypes, boolean enableVoid) {
		fEnableBaseTypes= enableBaseTypes;
		fEnableVoid= enableVoid;
		fComparator= new JavaCompletionProposalComparator();
		
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet = triggers.toCharArray();
	}

	/**
	 * @param currPackage the new completion context
	 */
	public void setPackageFragment(IPackageFragment packageFragment) {
		fPackageFragment= packageFragment;
	}

	/**
	 * Computing proposals on a <code>ITextViewer</code> is not supported.
	 * @see #computeCompletionProposals(IContentAssistSubject, int)
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}
	
	/**
	 * Computing context information on a <code>ITextViewer</code> is not supported.
	 * @see #computeContextInformation(IContentAssistSubject, int)
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
		if (fPackageFragment == null)
			return null;
		String input= contentAssistSubjectControl.getDocument().get();
		if (documentOffset == 0)
			return null;
		ICompletionProposal[] proposals= internalComputeCompletionProposals(documentOffset, input);
		Arrays.sort(proposals, fComparator);
		return proposals;
	}

	private ICompletionProposal[] internalComputeCompletionProposals(int documentOffset, String input) {
		String cuString= CU_START + input + CU_END;
		ICompilationUnit cu= fPackageFragment.getCompilationUnit(CU_NAME);
		try {
			/*
			 * Explicitly create a new non-shared working copy.
			 */
			cu= cu.getWorkingCopy(new NullProgressMonitor());
			cu.getBuffer().setContents(cuString);
			int cuPrefixLength= CU_START.length();
			TypeCompletionCollector collector= new TypeCompletionCollector(cuPrefixLength, fPackageFragment.getJavaProject(), fEnableBaseTypes, fEnableVoid);
			cu.codeComplete(cuPrefixLength + documentOffset, collector);
			
			JavaCompletionProposal[] proposals= collector.getResults();
			if (proposals.length == 0) {
				String errorMsg= collector.getErrorMessage();
				if (errorMsg == null || errorMsg.trim().length() == 0)
					errorMsg= RefactoringMessages.getString("JavaTypeCompletionProcessor.no_completion");  //$NON-NLS-1$
				fErrorMessage= errorMsg;
			} else {
				fErrorMessage= collector.getErrorMessage();
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

	protected static JavaCompletionProposal createTypeCompletion(int start, int end, String completion, ImageDescriptor descriptor, String typeName, String containerName, ProposalInfo proposalInfo, int relevance) {
		String fullName= JavaModelUtil.concatenateName(containerName, typeName); // containername can be null
		
		StringBuffer buf= new StringBuffer(Signature.getSimpleName(fullName));
		String typeQualifier= Signature.getQualifier(fullName);
		buf.append(" - "); //$NON-NLS-1$
		if (typeQualifier.length() > 0) {
			buf.append(typeQualifier);
		} else if (containerName != null) {
			buf.append(RefactoringMessages.getString("JavaTypeCompletionProcessor.default_package")); //$NON-NLS-1$
		}
		String name= buf.toString();
		
		if ("java.lang".equals(containerName) && typeName.equals(completion)) //$NON-NLS-1$
			completion= containerName + '.' + completion; //since JDT core swallows java.lang
		
		JavaCompletionProposal proposal= new JavaTypeCompletionProposal(completion, null, start, end-start,
			getImage(descriptor), name, relevance, typeName, containerName);
		proposal.setProposalInfo(proposalInfo);
		return proposal;
	}

	private static Image getImage(ImageDescriptor descriptor) {
		return (descriptor == null) ? null : JavaTypeCompletionProcessor.IMAGE_DESC_REGISTRY.get(descriptor);
	}

	private static class TypeCompletionCollector extends CompletionRequestorAdapter {
		private int fOffsetReduction;
		private IJavaProject fJavaProject;
		private boolean fEnableBaseTypes2;
		private boolean fEnableVoid2;
		
		private List fProposals = new ArrayList();
		private String fErrorMessage2;

		public TypeCompletionCollector(int offsetReduction, IJavaProject javaProject, boolean enableBaseTypes, boolean enableVoid) {
			fOffsetReduction= offsetReduction;
			fJavaProject= javaProject;
			fEnableBaseTypes2= enableBaseTypes;
			fEnableVoid2= enableVoid;
		}

		public void acceptClass(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end, int relevance) {
			if (isDummyClass(typeName))
				return;
			ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(false, false, false, modifiers);
			ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
			fProposals.add(createAdjustedTypeCompletion(start, end, new String(completionName), descriptor, new String(typeName), new String(packageName), info, relevance));
		}
		
		public void acceptError(IProblem error) {
			fErrorMessage2= error.getMessage();
		}
		
		public void acceptInterface(char[] packageName, char[] typeName, char[] completionName, int modifiers, int start, int end, int relevance) {
			if (isDummyClass(typeName))
				return;
			ImageDescriptor descriptor= JavaElementImageProvider.getTypeImageDescriptor(true, false, false, modifiers);
			ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
			fProposals.add(createAdjustedTypeCompletion(start, end, new String(completionName), descriptor, new String(typeName), new String(packageName), info, relevance));
		}
		
		public void acceptKeyword(char[] keywordName, int start, int end, int relevance) {
			if (! fEnableBaseTypes2)
				return;
			String keyword= new String(keywordName);
			if ( (fEnableVoid2 && VOID.equals(keyword)) || (fEnableBaseTypes2 && BASE_TYPES.contains(keyword)) )
				fProposals.add(createAdjustedCompletion(start, end, keyword, null, keyword, relevance));
		}
		
		public void acceptPackage(char[] packageName, char[] completionName, int start, int end, int relevance) {
			fProposals.add(createAdjustedCompletion(start, end, new String(completionName), JavaPluginImages.DESC_OBJS_PACKAGE, new String(packageName), relevance));
		}
		
		public void acceptType(char[] packageName, char[] typeName, char[] completionName, int start, int end, int relevance) {
			if (isDummyClass(typeName))
				return;
			ProposalInfo info= new ProposalInfo(fJavaProject, packageName, typeName);
			fProposals.add(createAdjustedTypeCompletion(start, end, new String(completionName), JavaPluginImages.DESC_OBJS_CLASS, new String(typeName), new String(packageName), info, relevance));
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
		
		protected JavaCompletionProposal createAdjustedCompletion(int start, int end, String completion, ImageDescriptor descriptor, String name, int relevance) {
			return new JavaCompletionProposal(completion, start - fOffsetReduction, end-start, getImage(descriptor), name, relevance);
		}
		
		protected JavaCompletionProposal createAdjustedTypeCompletion(int start, int end, String completion, ImageDescriptor descriptor, String typeName, String containerName, ProposalInfo proposalInfo, int relevance) {
			return createTypeCompletion(start - fOffsetReduction, end - fOffsetReduction, completion, descriptor, typeName, containerName, proposalInfo, relevance);
		}
	}
}
