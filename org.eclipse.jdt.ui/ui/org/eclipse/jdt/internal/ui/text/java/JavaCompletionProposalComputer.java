/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;

import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.CompletionTimeoutProgressMonitor;
import org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.jdt.internal.ui.text.Symbols;

/**
 * Computes Java completion proposals and context infos.
 *
 * @since 3.2
 */
public class JavaCompletionProposalComputer implements IJavaCompletionProposalComputer {

	private static final class ContextInformationWrapper implements IContextInformation, IContextInformationExtension {

		private final IContextInformation fContextInformation;
		private int fPosition;

		public ContextInformationWrapper(IContextInformation contextInformation) {
			fContextInformation= contextInformation;
		}

		/*
		 * @see IContextInformation#getContextDisplayString()
		 */
		@Override
		public String getContextDisplayString() {
			return fContextInformation.getContextDisplayString();
		}

			/*
		 * @see IContextInformation#getImage()
		 */
		@Override
		public Image getImage() {
			return fContextInformation.getImage();
		}

		/*
		 * @see IContextInformation#getInformationDisplayString()
		 */
		@Override
		public String getInformationDisplayString() {
			return fContextInformation.getInformationDisplayString();
		}

		/*
		 * @see IContextInformationExtension#getContextInformationPosition()
		 */
		@Override
		public int getContextInformationPosition() {
			return fPosition;
		}

		public void setContextInformationPosition(int position) {
			fPosition= position;
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.IContextInformation#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object object) {
			if (object instanceof ContextInformationWrapper) {
				return fContextInformation.equals(((ContextInformationWrapper) object).fContextInformation);
			} else {
				return fContextInformation.equals(object);
			}
		}

		/*
		 * @see java.lang.Object#hashCode()
		 * @since 3.5
		 */
		@Override
		public int hashCode() {
			return fContextInformation.hashCode();
		}
	}

	private String fErrorMessage;

	public JavaCompletionProposalComputer() {
	}

	protected int guessContextInformationPosition(ContentAssistInvocationContext context) {
		return context.getInvocationOffset();
	}

	protected final int guessMethodContextInformationPosition(ContentAssistInvocationContext context) {
		final int contextPosition= context.getInvocationOffset();

		IDocument document= context.getDocument();
		JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
		int bound= Math.max(-1, contextPosition - 2000);

		// try the innermost scope of parentheses that looks like a method call
		int pos= contextPosition - 1;
		do {
			int paren= scanner.findOpeningPeer(pos, bound, '(', ')');
			if (paren == JavaHeuristicScanner.NOT_FOUND) {
				break;
			}
			int token= scanner.previousToken(paren - 1, bound);
			// next token must be a method name (identifier) or the closing angle of a
			// constructor call of a parameterized type.
			if (token == Symbols.TokenIDENT || token == Symbols.TokenGREATERTHAN) {
				return paren + 1;
			}
			pos= paren - 1;
		} while (true);

		return contextPosition;
	}

	private List<IContextInformation> addContextInformations(JavaContentAssistInvocationContext context, int offset) {
		List<ICompletionProposal> proposals= internalComputeCompletionProposals(offset, context);
		List<IContextInformation> result= new ArrayList<>(proposals.size());
		List<IContextInformation> anonymousResult= new ArrayList<>(proposals.size());

		for (ICompletionProposal proposal : proposals) {
			IContextInformation contextInformation= proposal.getContextInformation();
			if (contextInformation != null) {
				ContextInformationWrapper wrapper= new ContextInformationWrapper(contextInformation);
				wrapper.setContextInformationPosition(offset);
				if (proposal instanceof AnonymousTypeCompletionProposal) {
					anonymousResult.add(wrapper);
				} else {
					result.add(wrapper);
				}
			}
		}

		if (result.isEmpty()) {
			return anonymousResult;
		}
		return result;

	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;

			int contextInformationPosition= guessContextInformationPosition(javaContext);
			List<IContextInformation> result= addContextInformations(javaContext, contextInformationPosition);
			return result;
		}
		return Collections.emptyList();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeCompletionProposals(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
			return internalComputeCompletionProposals(context.getInvocationOffset(), javaContext);
		}
		return Collections.emptyList();
	}

	private List<ICompletionProposal> internalComputeCompletionProposals(int offset, JavaContentAssistInvocationContext context) {
		ICompilationUnit unit= context.getCompilationUnit();
		if (unit == null) {
			return Collections.emptyList();
		}

		CompletionProposalCollector collector= createCollector(context);
		collector.setInvocationContext(context);

		// Allow completions for unresolved types - since 3.3
		collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF, true);
		collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_IMPORT, true);
		collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.FIELD_IMPORT, true);

		collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF, true);
		collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_IMPORT, true);
		collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.METHOD_IMPORT, true);

		collector.setAllowsRequiredProposals(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);

		collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);
		collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, CompletionProposal.TYPE_REF, true);

		collector.setAllowsRequiredProposals(CompletionProposal.TYPE_REF, CompletionProposal.TYPE_REF, true);

		// Set the favorite list to propose static members - since 3.3
		collector.setFavoriteReferences(getFavoriteStaticMembers());

		try {
			ITextSelection selection= context.getTextSelection();
			if (selection != null && selection.getLength() > 0) {
				collector.setReplacementLength(selection.getLength());
			}
			unit.codeComplete(offset, collector, createProgressMonitor(context));
		} catch (OperationCanceledException x) {
			IBindingService bindingSvc= PlatformUI.getWorkbench().getAdapter(IBindingService.class);
			String keyBinding= bindingSvc.getBestActiveBindingFormattedFor(IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST);
			fErrorMessage= Messages.format(JavaTextMessages.CompletionProcessor_error_javaCompletion_took_too_long_message, keyBinding);
		} catch (JavaModelException x) {
			final StyledText widget = context.getViewer().getTextWidget();
			Consumer<Control> popup = control -> {
				if (x.isDoesNotExist() && !unit.getJavaProject().isOnClasspath(unit)) {
					MessageDialog.openInformation(control.getShell(), JavaTextMessages.CompletionProcessor_error_notOnBuildPath_title, JavaTextMessages.CompletionProcessor_error_notOnBuildPath_message);
				} else {
					ErrorDialog.openError(control.getShell(), JavaTextMessages.CompletionProcessor_error_accessing_title, JavaTextMessages.CompletionProcessor_error_accessing_message, x.getStatus());
				}
			};
			if (Display.getCurrent() == null && Display.getDefault() != null) {
				Display.getDefault().asyncExec(() -> {
					popup.accept(widget);
				});
			} else {
				popup.accept(widget);
			}
		}

		ICompletionProposal[] javaProposals= collector.getJavaCompletionProposals();
		int contextInformationOffset= guessMethodContextInformationPosition(context);
		if (contextInformationOffset != offset) {
			for (ICompletionProposal javaProposal : javaProposals) {
				if (javaProposal instanceof JavaMethodCompletionProposal) {
					JavaMethodCompletionProposal jmcp= (JavaMethodCompletionProposal) javaProposal;
					jmcp.setContextInformationPosition(contextInformationOffset);
				}
			}
		}

		List<ICompletionProposal> proposals= new ArrayList<>(Arrays.asList(javaProposals));
		if (proposals.size() == 0) {
			String error= collector.getErrorMessage();
			if (error.length() > 0) {
				fErrorMessage= error;
			}
		}
		return proposals;
	}

	/**
	 * Returns the array with favorite static members.
	 *
	 * @return the <code>String</code> array with with favorite static members
	 * @see CompletionRequestor#setFavoriteReferences(String[])
	 * @since 3.3
	 */
	private String[] getFavoriteStaticMembers() {
		String serializedFavorites= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
		if (serializedFavorites != null && serializedFavorites.length() > 0) {
			return serializedFavorites.split(";"); //$NON-NLS-1$
		}
		return new String[0];
	}

	/**
	 * Creates the collector used to get proposals from core.
	 *
	 * @param context the context
	 * @return the collector
	 */
	protected CompletionProposalCollector createCollector(JavaContentAssistInvocationContext context) {
		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES)) {
			return new FillArgumentNamesCompletionProposalCollector(context);
		} else {
			return new CompletionProposalCollector(context.getCompilationUnit(), true);
		}
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/*
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionStarted()
	 */
	@Override
	public void sessionStarted() {
	}

	/*
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionEnded()
	 */
	@Override
	public void sessionEnded() {
		fErrorMessage= null;
	}

	private IProgressMonitor createProgressMonitor(JavaContentAssistInvocationContext context) {
		if (context.getViewer() instanceof JavaSourceViewer && ((JavaSourceViewer) context.getViewer()).isAsyncCompletionActive()) {
			return new NullProgressMonitor();
		}
		return new CompletionTimeoutProgressMonitor();
	}
}
