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
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Bin to collect the proposal of the infrastructure on code assist in a java text.
 */
public class ExperimentalResultCollector extends ResultCollector {

	protected JavaCompletionProposal createMethodCallCompletion(CompletionProposal methodProposal, String parameterList) {
		char[] completion= methodProposal.getCompletion();
		// super class' behavior if this is not a normal completion or has no
		// parameters
		if ((completion.length == 0) || ((completion.length == 1) && completion[0] == ')') || parameterList.length() == 0)
			return super.createMethodCallCompletion(methodProposal, parameterList);

		ImageDescriptor descriptor= createMemberDescriptor(methodProposal.getFlags());
		Image image= getImage(descriptor);
		String displayName= createMethodDisplayString(methodProposal, parameterList).toString();
		int start= methodProposal.getReplaceStart();
		int end= methodProposal.getReplaceEnd();
		int relevance= methodProposal.getRelevance();
		String name= String.valueOf(methodProposal.getName());
		char[] signature= methodProposal.getSignature();
		char[][] parameterNames= methodProposal.findParameterNames(null);

		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS)) {
			return new ParameterGuessingProposal(name, signature, start, end - start, image, displayName, fTextViewer, relevance, parameterNames, fCodeAssistOffset, fCompilationUnit);
		} else {
			ExperimentalProposal experimental= new ExperimentalProposal(name, signature, parameterNames, start, end - start, image, displayName, fTextViewer, relevance);
			return experimental;
		}

	}
	
	protected JavaCompletionProposal createTypeCompletion(CompletionProposal typeProposal) {
		char[] signature= typeProposal.getSignature();
		char[] packageName= Signature.getSignatureQualifier(signature); 
		char[] typeName= Signature.getSignatureSimpleName(signature);
		char[] completion= typeProposal.getCompletion();

		JavaCompletionProposal proposal= super.createTypeCompletion(typeProposal);
		// don't add parameters for import-completions
		if (completion.length > 0 && completion[completion.length - 1] == ';')
			return proposal;
		
		JavaCompletionProposal newProposal= new GenericJavaTypeProposal(proposal.getReplacementString(), fCompilationUnit, typeProposal.getReplaceStart(), typeProposal.getReplaceEnd() - typeProposal.getReplaceStart(), proposal.getImage(), proposal.getDisplayString(), fTextViewer, proposal.getRelevance(), typeProposal.getSignature(), String.valueOf(typeName), String.valueOf(packageName));
		newProposal.setProposalInfo(proposal.getProposalInfo());
		return newProposal;
	}
}
