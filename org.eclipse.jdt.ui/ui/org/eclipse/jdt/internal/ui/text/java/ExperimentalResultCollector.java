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

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.ResultCollector;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public final class ExperimentalResultCollector extends ResultCollector {

	public ExperimentalResultCollector(ICompilationUnit cu) {
		super(cu);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.ResultCollector#createJavaCompletionProposal(org.eclipse.jdt.core.CompletionProposal)
	 */
	protected IJavaCompletionProposal createJavaCompletionProposal(CompletionProposal proposal) {
		if (proposal.getKind() == CompletionProposal.METHOD_REF)
			return createMethodReferenceProposal(proposal);
		if (proposal.getKind() == CompletionProposal.TYPE_REF)
			return createTypeProposal(proposal);
		return super.createJavaCompletionProposal(proposal);
	}

	private IJavaCompletionProposal createMethodReferenceProposal(CompletionProposal methodProposal) {
		String completion= String.valueOf(methodProposal.getCompletion());
		// super class' behavior if this is not a normal completion or has no
		// parameters
		if ((completion.length() == 0) || ((completion.length() == 1) && completion.charAt(0) == ')') || Signature.getParameterCount(methodProposal.getSignature()) == 0)
			return super.createJavaCompletionProposal(methodProposal);

		Image image= getImage(getLabelProvider().createImageDescriptor(methodProposal));
		String displayName= getLabelProvider().createLabel(methodProposal);
		int start= methodProposal.getReplaceStart();
		int end= methodProposal.getReplaceEnd();
		int relevance= methodProposal.getRelevance();
		String name= String.valueOf(methodProposal.getName());
		
		
		char[] signature= methodProposal.getSignature();
		char[][] parameterNames= methodProposal.findParameterNames(null);

		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		JavaCompletionProposal proposal;
		if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS))
			proposal= new ParameterGuessingProposal(name, signature, start, end - start, image, displayName, relevance, parameterNames, methodProposal.getCompletionLocation() + 1, getCompilationUnit());
		else
			proposal= new ExperimentalProposal(name, signature, parameterNames, start, end - start, image, displayName, relevance);

		IJavaProject project= getCompilationUnit().getJavaProject();
		if (project != null)
			proposal.setProposalInfo(new MethodProposalInfo(project, methodProposal));

		char[] completionName= methodProposal.getCompletion();
		IContextInformation contextInformation= createMethodContextInformation(methodProposal);
		proposal.setContextInformation(contextInformation);
		proposal.setTriggerCharacters(METHOD_WITH_ARGUMENTS_TRIGGERS);
		
		if (completionName.length > 0) {
			// set the cursor before the closing bracket
			proposal.setCursorPosition(completionName.length - 1);
		}
		return proposal;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.ResultCollector#createTypeCompletion(org.eclipse.jdt.core.CompletionProposal)
	 */
	private IJavaCompletionProposal createTypeProposal(CompletionProposal typeProposal) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		if (!shouldProposeGenerics(project))
			return super.createJavaCompletionProposal(typeProposal);

		String completion= String.valueOf(typeProposal.getCompletion());
		// don't add parameters for import-completions
		if (completion.length() > 0 && completion.endsWith(";")) //$NON-NLS-1$
			return super.createJavaCompletionProposal(typeProposal);

		int start= typeProposal.getReplaceStart();
		int length= getLength(typeProposal);
		Image image= getImage(getLabelProvider().createImageDescriptor(typeProposal));
		String label= getLabelProvider().createLabel(typeProposal);
		
		JavaCompletionProposal newProposal= new GenericJavaTypeProposal(typeProposal, getContext(), start, length, getCompilationUnit(), image, label);
		if (project != null)
			newProposal.setProposalInfo(new TypeProposalInfo(project, typeProposal));
		
		newProposal.setTriggerCharacters(TYPE_TRIGGERS);
		return newProposal;
	}

	/**
	 * Returns <code>true</code> if generic proposals should be allowed,
	 * <code>false</code> if not. Note that even though code (in a library)
	 * may be referenced that uses generics, it is still possible that the
	 * current source does not allow generics.
	 * TODO move to subclass
	 * 
	 * @return <code>true</code> if the generic proposals should be allowed,
	 *         <code>false</code> if not
	 */
	private final boolean shouldProposeGenerics(IJavaProject project) {
		String sourceVersion;
		if (project != null)
			sourceVersion= project.getOption(JavaCore.COMPILER_SOURCE, true);
		else
			sourceVersion= JavaCore.getOption(JavaCore.COMPILER_SOURCE);
	
		return sourceVersion != null && JavaCore.VERSION_1_5.compareTo(sourceVersion) <= 0; 
	}
}
