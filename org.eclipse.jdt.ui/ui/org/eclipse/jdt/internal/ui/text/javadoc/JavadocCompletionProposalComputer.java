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

import org.eclipse.jdt.core.CompletionProposal;

import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComputer;

/**
 * 
 * @since 3.2
 */
public class JavadocCompletionProposalComputer extends JavaCompletionProposalComputer {
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComputer#createCollector(org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext)
	 */
	protected CompletionProposalCollector createCollector(JavaContentAssistInvocationContext context) {
		CompletionProposalCollector collector= super.createCollector(context);
		collector.setIgnored(CompletionProposal.ANNOTATION_ATTRIBUTE_REF, true);
		collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
		collector.setIgnored(CompletionProposal.FIELD_REF, false);
		collector.setIgnored(CompletionProposal.KEYWORD, true);
		collector.setIgnored(CompletionProposal.LABEL_REF, true);
		collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, true);
		collector.setIgnored(CompletionProposal.METHOD_DECLARATION, true);
		collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
		collector.setIgnored(CompletionProposal.METHOD_REF, false);
		collector.setIgnored(CompletionProposal.PACKAGE_REF, true);
		collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
		collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
		collector.setIgnored(CompletionProposal.JAVADOC_TYPE_REF, false);
		collector.setIgnored(CompletionProposal.JAVADOC_FIELD_REF, false);
		collector.setIgnored(CompletionProposal.JAVADOC_METHOD_REF, false);
		collector.setIgnored(CompletionProposal.JAVADOC_PARAM_REF, false);
		collector.setIgnored(CompletionProposal.JAVADOC_VALUE_REF, false);
		collector.setIgnored(CompletionProposal.TYPE_REF, false);
		return collector;
	}
}
