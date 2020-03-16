/**
 * Copyright (c) 2010, 2020 Darmstadt University of Technology and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.jdt.internal.ui.text.java;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.text.Chain;
import org.eclipse.jdt.internal.ui.text.ChainElement;
import org.eclipse.jdt.internal.ui.text.ChainElementAnalyzer;
import org.eclipse.jdt.internal.ui.text.ChainFinder;
import org.eclipse.jdt.internal.ui.text.ChainType;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal;

public class ChainCompletionProposalComputer implements IJavaCompletionProposalComputer {

	public static final String CATEGORY_ID= "org.eclipse.jdt.ui.javaChainProposalCategory"; //$NON-NLS-1$

	private JavaContentAssistInvocationContext ctx;

	private CompletionProposalCollector collector;

	private List<ChainElement> entrypoints;

	private String error;

	private String[] excludedTypes;

	@Override
	public List<ICompletionProposal> computeCompletionProposals(final ContentAssistInvocationContext context,
			final IProgressMonitor monitor) {

		if (!initializeRequiredContext(context)) {
			return Collections.emptyList();
		}
		if (!shouldPerformCompletionOnExpectedType()) {
			return Collections.emptyList();
		}
		return executeCallChainSearch();
	}

	private boolean initializeRequiredContext(final ContentAssistInvocationContext context) {
		if (!(context instanceof JavaContentAssistInvocationContext)) {
			return false;
		}

		ctx= (JavaContentAssistInvocationContext) context;
		collector= new CompletionProposalCollector(ctx.getCompilationUnit());
		collector.setInvocationContext(ctx);
		ICompilationUnit cu= ctx.getCompilationUnit();
		int offset= ctx.getInvocationOffset();
		try {
			cu.codeComplete(offset, collector, new NullProgressMonitor());
		} catch (JavaModelException e) {
			// try to continue
		}

		return true;
	}

	private boolean shouldPerformCompletionOnExpectedType() {
		if (ctx.getCoreContext() == null) {
			return false;
		}

		AST ast;
		CompilationUnit cuNode= SharedASTProviderCore.getAST(ctx.getCompilationUnit(), SharedASTProviderCore.WAIT_NO, null);
		if (cuNode != null) {
			ast= cuNode.getAST();
		} else {
			ast= ASTCreator.createAST(ctx.getCompilationUnit(), null).getAST();
		}
		return ast.resolveWellKnownType(ChainElementAnalyzer.getExpectedFullyQualifiedTypeName(ctx.getCoreContext())) != null
				|| ChainElementAnalyzer.getExpectedType(ctx.getProject(), ctx.getCoreContext()) != null;
	}

	private boolean findEntrypoints() {
		entrypoints= new LinkedList<>();
		for (IJavaCompletionProposal prop : collector.getJavaCompletionProposals()) {
			if (prop instanceof AbstractJavaCompletionProposal) {
				AbstractJavaCompletionProposal aprop= (AbstractJavaCompletionProposal) prop;
				IJavaElement e= aprop.getJavaElement();
				if (e != null) {
					if (matchesExpectedPrefix(e) && !ChainFinder.isFromExcludedType(Arrays.asList(excludedTypes), e)) {
						ChainElement ce= new ChainElement(e, false);
						if (ce.getElementType() != null) {
							entrypoints.add(ce);
						}
					}
				} else {
					IJavaElement[] visibleElements= ctx.getCoreContext().getVisibleElements(null);
					for (IJavaElement ve : visibleElements) {
						if (ve.getElementName().equals(aprop.getReplacementString()) && matchesExpectedPrefix(ve)
								&& !ChainFinder.isFromExcludedType(Arrays.asList(excludedTypes), ve)) {
							ChainElement ce= new ChainElement(ve, false);
							if (ce.getElementType() != null) {
								entrypoints.add(ce);
							}
						}
					}
				}
			}
		}

		return !entrypoints.isEmpty();
	}

	private boolean matchesExpectedPrefix(final IJavaElement element) {
		String prefix= String.valueOf(ctx.getCoreContext().getToken());
		return String.valueOf(element.getElementName()).startsWith(prefix);
	}

	private List<ICompletionProposal> executeCallChainSearch() {
		final int maxChains= Integer.parseInt(JavaManipulation.getPreference(PreferenceConstants.PREF_MAX_CHAINS, ctx.getProject()));
		final int minDepth= Integer.parseInt(JavaManipulation.getPreference(PreferenceConstants.PREF_MIN_CHAIN_LENGTH, ctx.getProject()));
		final int maxDepth= Integer.parseInt(JavaManipulation.getPreference(PreferenceConstants.PREF_MAX_CHAIN_LENGTH, ctx.getProject()));

		excludedTypes= JavaManipulation.getPreference(PreferenceConstants.PREF_CHAIN_IGNORED_TYPES, ctx.getProject()).split("\\|"); //$NON-NLS-1$
		for (int i= 0; i < excludedTypes.length; ++i) {
			excludedTypes[i]= "L" + excludedTypes[i].replace('.', '/'); //$NON-NLS-1$
		}

		final IType invocationType= ctx.getCompilationUnit().findPrimaryType();

		final List<ChainType> expectedTypes= ChainElementAnalyzer.resolveBindingsForExpectedTypes(ctx.getProject(), ctx.getCoreContext());
		final ChainFinder finder= new ChainFinder(expectedTypes, Arrays.asList(excludedTypes), invocationType);
		final ExecutorService executor= Executors.newSingleThreadExecutor();
		try {
			Future<?> future= executor.submit(() -> {
				if (findEntrypoints()) {
					finder.startChainSearch(entrypoints, maxChains, minDepth, maxDepth);
				}
			});
			long timeout= Long.parseLong(JavaManipulation.getPreference(PreferenceConstants.PREF_CHAIN_TIMEOUT, ctx.getProject()));
			future.get(timeout, TimeUnit.SECONDS);
		} catch (final Exception e) {
			finder.cancel();
			executor.shutdownNow();
			setError("Timeout during call chain computation."); //$NON-NLS-1$
		}
		return buildCompletionProposals(finder.getChains());
	}

	private List<ICompletionProposal> buildCompletionProposals(final List<Chain> chains) {
		final List<ICompletionProposal> proposals= new LinkedList<>();
		for (final Chain chain : chains) {
			final TemplateProposal proposal= ChainCompletionTemplateBuilder.create(chain, ctx);
			final ChainCompletionProposal completionProposal= new ChainCompletionProposal(proposal, chain);
			proposals.add(completionProposal);
		}
		return proposals;
	}

	private void setError(final String errorMessage) {
		error= errorMessage;
	}

	@Override
	public List<IContextInformation> computeContextInformation(final ContentAssistInvocationContext context,
			final IProgressMonitor monitor) {
		return Collections.emptyList();
	}

	@Override
	public void sessionStarted() {
		setError(null);
	}

	@Override
	public String getErrorMessage() {
		return error;
	}

	@Override
	public void sessionEnded() {
	}
}
