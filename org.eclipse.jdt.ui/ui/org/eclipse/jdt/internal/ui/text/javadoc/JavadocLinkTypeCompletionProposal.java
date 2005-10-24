/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.javadoc;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.ui.text.java.CompletionProposalLabelProvider;

import org.eclipse.jdt.internal.ui.text.java.LazyJavaTypeCompletionProposal;

/**
 * Types directly completed to &#x7b;&#x40;link Type&#x7d;. See {@link CompletionProposal#JAVADOC_TYPE_REF}.
 * 
 * @since 3.2
 */
public final class JavadocLinkTypeCompletionProposal extends LazyJavaTypeCompletionProposal {

	public JavadocLinkTypeCompletionProposal(CompletionProposal proposal, CompletionContext context, ICompilationUnit cu, CompletionProposalLabelProvider labelProvider) {
		super(proposal, context, cu, labelProvider);
		Assert.isTrue(context.isInJavadoc());
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaTypeCompletionProposal#computeReplacementString()
	 */
	protected String computeReplacementString() {
		String typeReplacement= super.computeReplacementString();
		// TODO respect the auto-close preference, but do so consistently with method completions
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=113544
//		if (autocloseBrackets())
		if (true)
			return "{@link " + typeReplacement + "}"; //$NON-NLS-1$ //$NON-NLS-2$
		else
			return "{@link " + typeReplacement; //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaTypeCompletionProposal#apply(org.eclipse.jface.text.IDocument, char, int)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		// convert . to #
		if (trigger == '.')
			trigger= '#';
		// TODO respect the auto-close preference, but do so consistently with method completions
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=113544
//		boolean continueWithMember= trigger == '#' && autocloseBrackets();
		boolean continueWithMember= trigger == '#';
		if (continueWithMember)
			setCursorPosition(getCursorPosition() - 1); // before the closing curly brace
		
		super.apply(document, trigger, offset);

		if (continueWithMember)
			setUpLinkedMode(document, '}');
	}
}
