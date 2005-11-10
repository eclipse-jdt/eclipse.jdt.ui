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

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class JavaMethodCompletionProposal extends LazyJavaCompletionProposal {
	/** Triggers for method proposals without parameters. Do not modify. */
	protected final static char[] METHOD_TRIGGERS= new char[] { ';', ',', '.', '\t', '[', ' ' };
	/** Triggers for method proposals. Do not modify. */
	protected final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS= new char[] { '(', '-', ' ' };
	/** Triggers for method name proposals (static imports). Do not modify. */
	protected final static char[] METHOD_NAME_TRIGGERS= new char[] { ';' };
	
	protected static final class FormatterPrefs {
		public final boolean beforeOpeningParen;
		public final boolean afterOpeningParen;
		public final boolean beforeComma;
		public final boolean afterComma;
		public final boolean beforeClosingParen;
		public final boolean inEmptyList;

		FormatterPrefs(IJavaProject project) {
			beforeOpeningParen= getCoreOption(project, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION, false);
			afterOpeningParen= getCoreOption(project, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION, false);
			beforeComma= getCoreOption(project, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, false);
			afterComma= getCoreOption(project, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, true);
			beforeClosingParen= getCoreOption(project, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION, false);
			inEmptyList= getCoreOption(project, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION, false);
		}

		private boolean getCoreOption(IJavaProject project, String key, boolean def) {
			String option= getCoreOption(project, key);
			if (JavaCore.INSERT.equals(option))
				return true;
			if (JavaCore.DO_NOT_INSERT.equals(option))
				return false;
			return def;
		}

		private String getCoreOption(IJavaProject project, String key) {
			if (project == null)
				return JavaCore.getOption(key);
			return project.getOption(key, true);
		}
	}
	
	protected static final String LPAREN= "("; //$NON-NLS-1$
	protected static final String RPAREN= ")"; //$NON-NLS-1$
	protected static final String COMMA= ","; //$NON-NLS-1$
	protected static final String SPACE= " "; //$NON-NLS-1$

	protected final ICompilationUnit fCompilationUnit;
	
	private boolean fHasParameters;
	private boolean fHasParametersComputed= false;
	private int fContextInformationPosition;
	private FormatterPrefs fFormatterPrefs;

	public JavaMethodCompletionProposal(CompletionProposal proposal, CompletionContext context, ICompilationUnit cu, CompletionProposalLabelProvider labelProvider) {
		super(proposal, context, labelProvider);
		fCompilationUnit= cu;
	}

	public void apply(IDocument document, char trigger, int offset) {
		super.apply(document, trigger, offset);
		if (needsLinkedMode()) {
			setUpLinkedMode(document, ')');
		}
	}

	protected boolean needsLinkedMode() {
		return hasArgumentList() && hasParameters();
	}
	
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return String.valueOf(fProposal.getName());
	}
	
	protected IContextInformation computeContextInformation() {
		// no context information for METHOD_NAME_REF proposals (e.g. for static imports)
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=94654
		if (fProposal.getKind() == CompletionProposal.METHOD_REF &&  hasParameters() && (getReplacementString().endsWith(RPAREN) || getReplacementString().length() == 0)) {
			ProposalContextInformation contextInformation= new ProposalContextInformation(fProposal);
			if (fContextInformationPosition != 0 && fProposal.getCompletion().length == 0)
				contextInformation.setContextInformationPosition(fContextInformationPosition);
			return contextInformation;
		}
		return super.computeContextInformation();
	}
	
	protected char[] computeTriggerCharacters() {
		if (fProposal.getKind() == CompletionProposal.METHOD_NAME_REFERENCE)
			return METHOD_NAME_TRIGGERS;
		if (hasParameters())
			return METHOD_WITH_ARGUMENTS_TRIGGERS;
		return METHOD_TRIGGERS;
	}
	
	/**
	 * Returns <code>true</code> if the method being inserted has at least one parameter. Note
	 * that this does not say anything about whether the argument list should be inserted. This
	 * depends on the position in the document and the kind of proposal; see
	 * {@link #hasArgumentList() }.
	 * 
	 * @return <code>true</code> if the method has any parameters, <code>false</code> if it has
	 *         no parameters
	 */
	protected final boolean hasParameters() {
		if (!fHasParametersComputed) {
			fHasParametersComputed= true;
			fHasParameters= computeHasParameters();
		}
		return fHasParameters;
	}

	private boolean computeHasParameters() throws IllegalArgumentException {
		return Signature.getParameterCount(fProposal.getSignature()) > 0;
	}

	/**
	 * Returns <code>true</code> if the argument list should be inserted by the proposal,
	 * <code>false</code> if not.
	 * 
	 * @return <code>true</code> when the the proposal is not in javadoc and comprises the
	 *         parameter list
	 */
	protected boolean hasArgumentList() {
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		boolean noOverwrite= preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION) ^ isToggleEating();
		char[] completion= fProposal.getCompletion();
		return !fContext.isInJavadoc() && (noOverwrite  || completion.length != 0 && completion[completion.length - 1] == ')');
	}

	/**
	 * Returns the method formatter preferences.
	 * 
	 * @return the formatter settings
	 */
	protected final FormatterPrefs getFormatterPrefs() {
		if (fFormatterPrefs == null)
			fFormatterPrefs= new FormatterPrefs(fCompilationUnit == null ? null : fCompilationUnit.getJavaProject());
		return fFormatterPrefs;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#computeReplacementString()
	 */
	protected String computeReplacementString() {
		if (!hasArgumentList())
			return super.computeReplacementString();
		
		// we're inserting a method plus the argument list - respect formatter preferences
		StringBuffer buffer= new StringBuffer();
		buffer.append(fProposal.getName());

		FormatterPrefs prefs= getFormatterPrefs();
		if (prefs.beforeOpeningParen)
			buffer.append(SPACE);
		buffer.append(LPAREN);
		
		if (hasParameters()) {
			setCursorPosition(buffer.length());
			
			if (prefs.afterOpeningParen)
				buffer.append(SPACE);
			

			// don't add the trailing space, but let the user type it in himself - typing the closing paren will exit
//			if (prefs.beforeClosingParen)
//				buffer.append(SPACE);
		} else {
			if (prefs.inEmptyList)
				buffer.append(SPACE);
		}

		buffer.append(RPAREN);

		return buffer.toString();

	}
	
	protected ProposalInfo computeProposalInfo() {
		if (fCompilationUnit != null) {
			IJavaProject project= fCompilationUnit.getJavaProject();
			if (project != null)
				return new MethodProposalInfo(project, fProposal);
		}
		return super.computeProposalInfo();
	}
	
	/**
	 * Overrides the default context information position. Ignored if set to zero.
	 * 
	 * @param contextInformationPosition the replaced position.
	 */
	public void setContextInformationPosition(int contextInformationPosition) {
		fContextInformationPosition= contextInformationPosition;
	}
}
