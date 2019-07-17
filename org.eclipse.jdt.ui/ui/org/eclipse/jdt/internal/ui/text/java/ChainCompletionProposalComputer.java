/**
 * Copyright (c) 2010, 2019 Darmstadt University of Technology and others
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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
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
import org.eclipse.jdt.internal.ui.text.ChainFinder;
import org.eclipse.jdt.internal.ui.text.TypeBindingAnalyzer;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal;

public class ChainCompletionProposalComputer implements IJavaCompletionProposalComputer {

	public static final String CATEGORY_ID= "org.eclipse.jdt.ui.javaChainProposalCategory"; //$NON-NLS-1$

	private JavaContentAssistInvocationContext ctx;

	private CompletionProposalCollector collector;

	private List<ChainElement> entrypoints;

	private String error;

	private IJavaElement invocationSite;

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

		invocationSite= ctx.getCoreContext().getEnclosingElement();
		return true;
	}

	private boolean shouldPerformCompletionOnExpectedType() {
		AST ast;
		CompilationUnit cuNode= SharedASTProviderCore.getAST(ctx.getCompilationUnit(), SharedASTProviderCore.WAIT_NO, null);
		if (cuNode != null) {
			ast= cuNode.getAST();
		} else {
			ast= ASTCreator.createAST(ctx.getCompilationUnit(), null).getAST();
		}
		ITypeBinding binding= ast.resolveWellKnownType(TypeBindingAnalyzer.getExpectedFullyQualifiedTypeName(ctx.getCoreContext()));
		return binding != null || TypeBindingAnalyzer.getExpectedType(ctx.getProject(), ctx.getCoreContext()) != null;
	}

	private boolean findEntrypoints() {
		entrypoints= new LinkedList<>();
		List<IJavaElement> nonTypeElements= new LinkedList<>();
		List<IType> typeElements= new LinkedList<>();
		for (IJavaCompletionProposal prop : collector.getJavaCompletionProposals()) {
			if (prop instanceof AbstractJavaCompletionProposal) {
				AbstractJavaCompletionProposal aprop= (AbstractJavaCompletionProposal) prop;
				IJavaElement element= aprop.getJavaElement();
				if (element != null && matchesExpectedPrefix(element)) {
					if (element instanceof IType) {
						if (hasPublicStaticNonPrimitiveMember((IType) element)) {
							typeElements.add((IType) element);
						}
					} else {
						if (element instanceof IMethod) {
							if (isNonPrimitiveMethod((IMethod) element)) {
								nonTypeElements.add(element);
							}
						} else if (element instanceof IField) {
							if (isNonPrimitiveField((IField) element)) {
								nonTypeElements.add(element);
							}
						} else {
							nonTypeElements.add(element);
						}
					}
				} else {
					IJavaElement[] visibleElements= ctx.getCoreContext().getVisibleElements(null);
					for (IJavaElement ve : visibleElements) {
						if (ve.getElementName().equals(aprop.getReplacementString()) && matchesExpectedPrefix(ve)) {
							if (ve instanceof IType ) {
								if (hasPublicStaticNonPrimitiveMember((IType) ve)) {
									typeElements.add((IType) ve);
								}
							} else {
								if (ve instanceof IMethod) {
									if (isNonPrimitiveMethod((IMethod) ve)) {
										nonTypeElements.add(ve);
									}
								} else if (element instanceof IField) {
									if (isNonPrimitiveField((IField) element)) {
										nonTypeElements.add(ve);
									}
								} else {
									nonTypeElements.add(ve);
								}
							}
						}
					}
				}
			}
		}

		List<IBinding> bindings= new LinkedList<>();
		IBinding [] tmp;
		if (!nonTypeElements.isEmpty()) {
			tmp= TypeBindingAnalyzer.resolveBindingsForElements(ctx.getCompilationUnit(), nonTypeElements.toArray(new IJavaElement[0]), false);
			bindings.addAll(Arrays.asList(tmp));
		}
		if (!typeElements.isEmpty()) {
			tmp= TypeBindingAnalyzer.resolveBindingsForElements(ctx.getCompilationUnit(), typeElements.toArray(new IJavaElement[0]), true);
			bindings.addAll(Arrays.asList(tmp));
		}
		for (IBinding b : bindings) {
			if (b != null && !ChainFinder.isFromExcludedType(Arrays.asList(excludedTypes), b)) {
				entrypoints.add(new ChainElement(b, false));
			}
		}

		return !entrypoints.isEmpty();
	}

	private static boolean hasPublicStaticNonPrimitiveMember(IType element) {
		try {
			for (IMethod m : element.getMethods()) {
				int flags= m.getFlags();
				if (Flags.isStatic(flags) && Flags.isPublic(flags) && !Signature.SIG_VOID.equals(m.getReturnType())) {
					int returnType= Signature.getTypeSignatureKind(m.getReturnType());
					if (returnType != Signature.BASE_TYPE_SIGNATURE) {
						return true;
					}
				}
			}
			for (IField f : element.getFields()) {
				int flags= f.getFlags();
				if (Flags.isStatic(flags) && Flags.isPublic(flags)) {
					int typeSignature= Signature.getTypeSignatureKind(f.getTypeSignature());
					if (typeSignature != Signature.BASE_TYPE_SIGNATURE) {
						return true;
					}
				}
			}
		} catch (JavaModelException e) {
			// do nothing
		}

		return false;
    }

	private static boolean isNonPrimitiveMethod (IMethod method) {
		try {
			if (!Signature.SIG_VOID.equals(method.getReturnType()) && !method.isConstructor()) {
				int typeSignature= Signature.getTypeSignatureKind(method.getReturnType());
				if (typeSignature != Signature.BASE_TYPE_SIGNATURE) {
					return true;
				}
			}
		} catch (JavaModelException e) {
			// do nothing
		}
		return false;
	}

	private static boolean isNonPrimitiveField (IField field) {
		try {
			int typeSignature= Signature.getTypeSignatureKind(field.getTypeSignature());
			if (typeSignature != Signature.BASE_TYPE_SIGNATURE) {
				return true;
			}
		} catch (JavaModelException e) {
			// do nothing
		}
		return false;
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

		final IType invocationType= ((IMember) invocationSite).getCompilationUnit().findPrimaryType();
		ITypeBinding receiverType= TypeBindingAnalyzer.getTypeBindingFrom(invocationType);

		final List<ITypeBinding> expectedTypes= TypeBindingAnalyzer.resolveBindingsForExpectedTypes(ctx.getProject(), ctx.getCompilationUnit(), ctx.getCoreContext());
		final ChainFinder finder= new ChainFinder(expectedTypes, Arrays.asList(excludedTypes), receiverType);
		try {
			ExecutorService executor= Executors.newSingleThreadExecutor();
			Future<?> future= executor.submit(() -> {
				if (findEntrypoints()) {
					finder.startChainSearch(entrypoints, maxChains, minDepth, maxDepth);
				}
			});
			long timeout= Long.parseLong(JavaManipulation.getPreference(PreferenceConstants.PREF_CHAIN_TIMEOUT, ctx.getProject()));
			future.get(timeout, TimeUnit.SECONDS);
		} catch (final Exception e) {
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
