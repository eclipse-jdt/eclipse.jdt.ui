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

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public class ExperimentalResultCollector extends ResultCollector {

	protected IJavaCompletionProposal createMethodReferenceProposal(CompletionProposal methodProposal) {
		String completion= String.valueOf(methodProposal.getCompletion());
		// super class' behavior if this is not a normal completion or has no
		// parameters
		if ((completion.length() == 0) || ((completion.length() == 1) && completion.charAt(0) == ')') || Signature.getParameterCount(methodProposal.getSignature()) == 0)
			return super.createMethodReferenceProposal(methodProposal);

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
			proposal= new ParameterGuessingProposal(name, signature, start, end - start, image, displayName, getTextViewer(), relevance, parameterNames, getCodeAssistOffset(), getCompilationUnit());
		else
			proposal= new ExperimentalProposal(name, signature, parameterNames, start, end - start, image, displayName, getTextViewer(), relevance);

		proposal.setProposalInfo(new MethodProposalInfo(getJavaProject(), methodProposal));

		char[] completionName= methodProposal.getCompletion();
		ProposalContextInformation contextInformation= createContextInformation(methodProposal);
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
	protected IJavaCompletionProposal createTypeProposal(CompletionProposal typeProposal) {
		if (!proposeGenerics())
			return super.createTypeProposal(typeProposal);

		String completion= String.valueOf(typeProposal.getCompletion());
		// don't add parameters for import-completions
		if (completion.length() > 0 && completion.endsWith(";")) //$NON-NLS-1$
			return super.createTypeProposal(typeProposal);

		int start= typeProposal.getReplaceStart();
		int length= getLength(start, typeProposal.getReplaceEnd());
		Image image= getImage(getLabelProvider().createImageDescriptor(typeProposal));
		String label= getLabelProvider().createLabel(typeProposal);
		
		JavaCompletionProposal newProposal= new GenericJavaTypeProposal(typeProposal, start, length, getCompilationUnit(), image, label, getTextViewer());
		newProposal.setProposalInfo(new TypeProposalInfo(getJavaProject(), typeProposal));
		
		newProposal.setTriggerCharacters(TYPE_TRIGGERS);
		return newProposal;
	}
}
