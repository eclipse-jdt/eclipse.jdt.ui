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

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal;

/**
 * Completions of inline tags such as &#x7b;&#x40;link &#x7d;. See {@link CompletionProposal#JAVADOC_INLINE_TAG}.
 * 
 * @since 3.2
 */
public final class JavadocInlineTagCompletionProposal extends LazyJavaCompletionProposal {
	/** Triggers for types in javadoc. Do not modify. */
	protected static final char[] JDOC_INLINE_TAG_TRIGGERS= new char[] { '#', '}', ' ' };

	public JavadocInlineTagCompletionProposal(CompletionProposal proposal, CompletionContext context) {
		super(proposal, context);
		Assert.isTrue(context.isInJavadoc());
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#computeReplacementString()
	 */
	protected String computeReplacementString() {
		String replacement= super.computeReplacementString();
		if (!autocloseBrackets() && replacement.endsWith("}")) //$NON-NLS-1$
			return replacement.substring(0, replacement.length() - 1);
		return replacement;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaTypeCompletionProposal#apply(org.eclipse.jface.text.IDocument, char, int)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		boolean needsLinkedMode= autocloseBrackets();
		if (needsLinkedMode)
			setCursorPosition(getCursorPosition() - 1); // before the closing curly brace
		
		super.apply(document, trigger, offset);

		if (needsLinkedMode)
			setUpLinkedMode(document, '}');
	}
}
