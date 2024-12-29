/*******************************************************************************
 * Copyright (c) 2005, 2024 IBM Corporation and others.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.jdt.internal.ui.text.Symbols;


/**
 *
 * @since 3.2
 */
public class JavaTypeCompletionProposalComputer extends JavaCompletionProposalComputer {
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComputer#createCollector(org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext)
	 */
	@Override
	protected CompletionProposalCollector createCollector(JavaContentAssistInvocationContext context) {
		CompletionProposalCollector collector= super.createCollector(context);
		collector.setIgnored(CompletionProposal.TYPE_REF, false);
		return collector;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComputer#computeCompletionProposals(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		List<ICompletionProposal> types= super.computeCompletionProposals(context, monitor);

		if (!(context instanceof JavaContentAssistInvocationContext))
			return types;

		JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
		CompletionContext coreContext= javaContext.getCoreContext();
		if (coreContext != null && coreContext.getTokenLocation() != CompletionContext.TL_CONSTRUCTOR_START)
			return types;

		try {
			if (types.size() > 0 && context.computeIdentifierPrefix().length() == 0) {
				IType expectedType= javaContext.getExpectedType();
				if (expectedType != null) {
					// empty prefix completion - insert LRU types if known, but prune if they already occur in the core list

					// compute minmimum relevance and already proposed list
					int relevance= Integer.MAX_VALUE;
					Set<String> proposed= new HashSet<>();
					for (ICompletionProposal iCompletionProposal : types) {
						AbstractJavaCompletionProposal p= (AbstractJavaCompletionProposal) iCompletionProposal;
						IJavaElement element= p.getJavaElement();
						if (element instanceof IType)
							proposed.add(((IType) element).getFullyQualifiedName());
						relevance= Math.min(relevance, p.getRelevance());
					}

					// insert history types
					List<String> history= JavaPlugin.getDefault().getContentAssistHistory().getHistory(expectedType.getFullyQualifiedName()).getTypes();
					relevance-= history.size() + 1;
					for (String type : history) {
						if (proposed.contains(type))
							continue;

						IJavaCompletionProposal proposal= createTypeProposal(relevance, type, javaContext);

						if (proposal != null)
							types.add(proposal);
						relevance++;
					}
				}
			}
		} catch (BadLocationException | JavaModelException x) {
			// log & ignore
			JavaPlugin.log(x);
		}

		return types;
	}

	private IJavaCompletionProposal createTypeProposal(int relevance, String fullyQualifiedType, JavaContentAssistInvocationContext context) throws JavaModelException {
		IType type= context.getCompilationUnit().getJavaProject().findType(fullyQualifiedType);
		if (type == null)
			return null;

		CompletionProposal proposal= CompletionProposal.create(CompletionProposal.TYPE_REF, context.getInvocationOffset());
		proposal.setCompletion(fullyQualifiedType.toCharArray());
		proposal.setDeclarationSignature(type.getPackageFragment().getElementName().toCharArray());
		proposal.setFlags(type.getFlags());
		proposal.setReplaceRange(context.getInvocationOffset(), context.getInvocationOffset());
		proposal.setSignature(Signature.createTypeSignature(fullyQualifiedType, true).toCharArray());

		LazyGenericTypeProposal p= new LazyGenericTypeProposal(proposal, context);
		p.setRelevance(relevance);
		return p;
	}

	@Override
	protected int guessContextInformationPosition(ContentAssistInvocationContext context) {
		final int contextPosition= context.getInvocationOffset();

		IDocument document= context.getDocument();
		JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
		int bound= Math.max(-1, contextPosition - 200);

		// try the innermost scope of angle brackets that looks like a generic type argument list
		try {
			int pos= contextPosition - 1;
			do {
				int angle= scanner.findOpeningPeer(pos, bound, '<', '>');
				if (angle == JavaHeuristicScanner.NOT_FOUND)
					break;
				int token= scanner.previousToken(angle - 1, bound);
				// next token must be a method name that is a generic type
				if (token == Symbols.TokenIDENT) {
					int off= scanner.getPosition() + 1;
					int end= angle;
					String ident= document.get(off, end - off).trim();
					if (JavaHeuristicScanner.isGenericStarter(ident))
						return angle + 1;
				}
				pos= angle - 1;
			} while (true);
		} catch (BadLocationException x) {
		}

		return super.guessContextInformationPosition(context);
	}

}
