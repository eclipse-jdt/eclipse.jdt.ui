/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Completion proposal collector which creates proposals with
 * filled in argument names.
 * <p>
 * This collector is used when
 * {@link PreferenceConstants#CODEASSIST_FILL_ARGUMENT_NAMES} is enabled.
 */
public final class FillArgumentNamesCompletionProposalCollector extends CompletionProposalCollector {

	private final boolean fIsGuessArguments;

	public FillArgumentNamesCompletionProposalCollector(JavaContentAssistInvocationContext context) {
		super(context.getCompilationUnit(), true);
		setInvocationContext(context);
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		fIsGuessArguments= preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
		if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES)) {
			setRequireExtendedContext(true);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.ResultCollector#createJavaCompletionProposal(org.eclipse.jdt.core.CompletionProposal)
	 */
	@Override
	protected IJavaCompletionProposal createJavaCompletionProposal(CompletionProposal proposal) {
		switch (proposal.getKind()) {
			case CompletionProposal.METHOD_REF:
			case CompletionProposal.CONSTRUCTOR_INVOCATION:
			case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER:
				return createMethodReferenceProposal(proposal);
			case CompletionProposal.TYPE_REF:
				return createTypeProposal(proposal);
			default:
				return super.createJavaCompletionProposal(proposal);
		}
	}

	private IJavaCompletionProposal createMethodReferenceProposal(CompletionProposal methodProposal) {
		String completion= String.valueOf(methodProposal.getCompletion());
		// super class' behavior if this is not a normal completion or has no
		// parameters
		if ((completion.length() == 0) || ((completion.length() == 1) && completion.charAt(0) == ')') || Signature.getParameterCount(methodProposal.getSignature()) == 0 || getContext().isInJavadoc())
			return super.createJavaCompletionProposal(methodProposal);

		LazyJavaCompletionProposal proposal= ParameterGuessingProposal.createProposal(methodProposal, getInvocationContext(), fIsGuessArguments);
		if (proposal == null) {
			proposal= new FilledArgumentNamesMethodProposal(methodProposal, getInvocationContext());
		}
		return proposal;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.ResultCollector#createTypeCompletion(org.eclipse.jdt.core.CompletionProposal)
	 */
	IJavaCompletionProposal createTypeProposal(CompletionProposal typeProposal) {
		final ICompilationUnit cu= getCompilationUnit();
		if (cu == null || getContext() != null && getContext().isInJavadoc())
			return super.createJavaCompletionProposal(typeProposal);

		char[] completion= typeProposal.getCompletion();
		// don't add parameters for import-completions nor for proposals with an empty completion (e.g. inside the type argument list)
		if (completion.length > 0 && completion[completion.length - 1] == '.')
			return super.createJavaCompletionProposal(typeProposal);
		if (getInvocationContext().getCoreContext().getTokenLocation() == CompletionContext.TL_IN_IMPORT)
			return super.createJavaCompletionProposal(typeProposal);

		LazyJavaCompletionProposal newProposal= new LazyGenericTypeProposal(typeProposal, getInvocationContext());
		return newProposal;
	}

}
