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
package org.eclipse.jdt.internal.ui.text.java;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * 
 * @since 3.2
 */
public class JavaTypeCompletionProposalComputer extends JavaCompletionProposalComputer {
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComputer#createCollector(org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext)
	 */
	protected CompletionProposalCollector createCollector(JavaContentAssistInvocationContext context) {
		CompletionProposalCollector collector= super.createCollector(context);
		collector.setIgnored(CompletionProposal.ANNOTATION_ATTRIBUTE_REF, true);
		collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
		collector.setIgnored(CompletionProposal.FIELD_REF, true);
		collector.setIgnored(CompletionProposal.KEYWORD, true);
		collector.setIgnored(CompletionProposal.LABEL_REF, true);
		collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, true);
		collector.setIgnored(CompletionProposal.METHOD_DECLARATION, true);
		collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
		collector.setIgnored(CompletionProposal.METHOD_REF, true);
		collector.setIgnored(CompletionProposal.PACKAGE_REF, true);
		collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
		collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
		
		collector.setIgnored(CompletionProposal.JAVADOC_BLOCK_TAG, true);
		collector.setIgnored(CompletionProposal.JAVADOC_FIELD_REF, true);
		collector.setIgnored(CompletionProposal.JAVADOC_INLINE_TAG, true);
		collector.setIgnored(CompletionProposal.JAVADOC_METHOD_REF, true);
		collector.setIgnored(CompletionProposal.JAVADOC_PARAM_REF, true);
		collector.setIgnored(CompletionProposal.JAVADOC_TYPE_REF, true);
		collector.setIgnored(CompletionProposal.JAVADOC_VALUE_REF, true);
		
		collector.setIgnored(CompletionProposal.TYPE_REF, false);
		return collector;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComputer#computeCompletionProposals(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public List computeCompletionProposals(TextContentAssistInvocationContext context, IProgressMonitor monitor) {
		List types= super.computeCompletionProposals(context, monitor);
		if (context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
			try {
				if (types.size() > 0 && context.computeIdentifierPrefix().length() == 0) {
					IType expectedType= javaContext.getExpectedType();
					if (expectedType != null) {
						// empty prefix completion - insert LRU types if known
						LazyJavaTypeCompletionProposal typeProposal= (LazyJavaTypeCompletionProposal) types.get(0);
						List history= JavaPlugin.getDefault().getContentAssistHistory().getHistory(expectedType.getFullyQualifiedName()).getTypes();
						
						int relevance= typeProposal.getRelevance() - history.size() - 1;
						for (Iterator it= history.iterator(); it.hasNext();) {
							String type= (String) it.next();
							if (type.equals(typeProposal.getProposedType().getFullyQualifiedName()))
								continue;
							
							IJavaCompletionProposal proposal= createTypeProposal(relevance, type, javaContext);
							
							if (proposal != null)
								types.add(proposal);
							relevance++;
						}
					}
				}
			} catch (BadLocationException x) {
				// log & ignore
				JavaPlugin.log(x);
			} catch (JavaModelException x) {
				// log & ignore
				JavaPlugin.log(x);
			}
		}
		return types;
	}

	private IJavaCompletionProposal createTypeProposal(int relevance, String fullyQualifiedType, JavaContentAssistInvocationContext context) throws JavaModelException {
		IType type= context.getCompilationUnit().getJavaProject().findType(fullyQualifiedType);
		if (type == null)
			return null;
		
		CompletionProposal proposal= CompletionProposal.create(CompletionProposal.TYPE_REF, context.getInvocationOffset());
		proposal.setCompletion(type.getElementName().toCharArray());
		proposal.setDeclarationSignature(type.getPackageFragment().getElementName().toCharArray());
		proposal.setFlags(type.getFlags());
		proposal.setRelevance(relevance);
		proposal.setReplaceRange(context.getInvocationOffset(), context.getInvocationOffset());
		proposal.setSignature(Signature.createTypeSignature(fullyQualifiedType, true).toCharArray());
		
		return new GenericJavaTypeProposal(proposal, context);
	}
}
