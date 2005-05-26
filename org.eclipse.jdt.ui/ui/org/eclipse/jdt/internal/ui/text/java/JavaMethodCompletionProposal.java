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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;

import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class JavaMethodCompletionProposal extends LazyJavaCompletionProposal {
	/** Triggers for method proposals without parameters. Do not modify. */
	protected final static char[] METHOD_TRIGGERS= new char[] { ';', ',', '.', '\t', '[', ' ' };
	/** Triggers for method proposals. Do not modify. */
	protected final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS= new char[] { '(', '-', ' ' };
	
	protected static class ExitPolicy implements IExitPolicy {
	
		final char fExitCharacter;
		private final IDocument fDocument;
	
		public ExitPolicy(char exitCharacter, IDocument document) {
			fExitCharacter= exitCharacter;
			fDocument= document;
		}
	
		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitPolicy#doExit(org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager, org.eclipse.swt.events.VerifyEvent, int, int)
		 */
		public ExitFlags doExit(LinkedModeModel environment, VerifyEvent event, int offset, int length) {
	
			if (event.character == fExitCharacter) {
				if (environment.anyPositionContains(offset))
					return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
				else
					return new ExitFlags(ILinkedModeListener.UPDATE_CARET, true);
			}
	
			switch (event.character) {
				case ';':
					return new ExitFlags(ILinkedModeListener.NONE, true);
				case SWT.CR:
					// when entering an anonymous class as a parameter, we don't want
					// to jump after the parenthesis when return is pressed
					if (offset > 0) {
						try {
							if (fDocument.getChar(offset - 1) == '{')
								return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
						} catch (BadLocationException e) {
						}
					}
					// fall through
				default:
					return null;
			}
		}
	
	}
	
	protected final ICompilationUnit fCompilationUnit;
	
	private boolean fHasParameters;
	private boolean fHasParametersComputed= false;
	private int fContextInformationPosition;

	public JavaMethodCompletionProposal(CompletionProposal proposal, ICompilationUnit cu) {
		super(proposal);
		fCompilationUnit= cu;
	}

	public void apply(IDocument document, char trigger, int offset) {
		super.apply(document, trigger, offset);
		try {
			setUpLinkedMode(document, getReplacementString());
		} catch (BadLocationException e) {
			// ignore
		}
	}

	private void setUpLinkedMode(IDocument document, String string) throws BadLocationException {
		if (getTextViewer() != null && string != null) {
			int index= string.indexOf("()"); //$NON-NLS-1$
			if (index != -1 && index + 1 == getCursorPosition()) {
				IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
				if (preferenceStore.getBoolean(PreferenceConstants.EDITOR_CLOSE_BRACKETS)) {
					int newOffset= getReplacementOffset() + getCursorPosition();

					LinkedPositionGroup group= new LinkedPositionGroup();
					group.addPosition(new LinkedPosition(document, newOffset, 0, LinkedPositionGroup.NO_STOP));

					LinkedModeModel model= new LinkedModeModel();
					model.addGroup(group);
					model.forceInstall();

					LinkedModeUI ui= new EditorLinkedModeUI(model, getTextViewer());
					ui.setSimpleMode(true);
					ui.setExitPolicy(new JavaMethodCompletionProposal.ExitPolicy(')', document));
					ui.setExitPosition(getTextViewer(), newOffset + 1, 0, Integer.MAX_VALUE);
					ui.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
					ui.enter();
				}
			}
		}
	}
	
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		String string= getReplacementString();
		int pos= string.indexOf('(');
		if (pos > 0)
			return string.subSequence(0, pos);
		else
			return string;
	}
	
	protected IContextInformation computeContextInformation() {
		// no context information for METHOD_NAME_REF proposals (e.g. for static imports)
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=94654
		if (fProposal.getKind() == CompletionProposal.METHOD_REF &&  hasParameters() && (getReplacementString().endsWith(")") || getReplacementString().length() == 0)) { //$NON-NLS-1$
			ProposalContextInformation contextInformation= new ProposalContextInformation(fProposal);
			if (fContextInformationPosition != 0 && fProposal.getCompletion().length == 0)
				contextInformation.setContextInformationPosition(fContextInformationPosition);
			return contextInformation;
		}
		return super.computeContextInformation();
	}
	
	protected char[] computeTriggerCharacters() {
		if (hasParameters())
			return METHOD_WITH_ARGUMENTS_TRIGGERS;
		return METHOD_TRIGGERS;
	}
	
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
	
	protected int computeCursorPosition() {
		if (hasParameters() && getReplacementString().endsWith(")")) //$NON-NLS-1$
			return getReplacementString().indexOf("(") + 1; //$NON-NLS-1$
		return super.computeCursorPosition();
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
