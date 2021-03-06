/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.actions.RenameJavaElementAction;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionCommandHandler;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;


/**
 * A quick assist proposal that starts the Rename refactoring.
 */
public class RenameRefactoringProposal implements IJavaCompletionProposal, ICompletionProposalExtension6, ICommandAccess {

	private final String fLabel;
	private int fRelevance;
	private final JavaEditor fEditor;

	public RenameRefactoringProposal(JavaEditor editor) {
		Assert.isNotNull(editor);
		fEditor= editor;
		fLabel= CorrectionMessages.RenameRefactoringProposal_name;
		fRelevance= IProposalRelevance.RENAME_REFACTORING;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	@Override
	public void apply(IDocument document) {
		RenameJavaElementAction renameAction= new RenameJavaElementAction(fEditor);
		renameAction.doRun(true);
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	@Override
	public String getAdditionalProposalInfo() {
		return CorrectionMessages.RenameRefactoringProposal_additionalInfo;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	@Override
	public String getDisplayString() {
		String shortCutString= CorrectionCommandHandler.getShortCutString(getCommandId());
		if (shortCutString != null) {
			return Messages.format(CorrectionMessages.ChangeCorrectionProposal_name_with_shortcut, new String[] { fLabel, shortCutString });
		}
		return fLabel;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension6#getStyledDisplayString()
	 */
	@Override
	public StyledString getStyledDisplayString() {
		StyledString str= new StyledString(fLabel);

		String shortCutString= CorrectionCommandHandler.getShortCutString(getCommandId());
		if (shortCutString != null) {
			String decorated= Messages.format(CorrectionMessages.ChangeCorrectionProposal_name_with_shortcut, new String[] { fLabel, shortCutString });
			return StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, str);
		}
		return str;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	@Override
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LINKED_RENAME);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	/*
	 * @see IJavaCompletionProposal#getRelevance()
	 */
	@Override
	public int getRelevance() {
		return fRelevance;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.IShortcutProposal#getProposalId()
	 */
	@Override
	public String getCommandId() {
		return IJavaEditorActionDefinitionIds.RENAME_ELEMENT;
	}

	public void setRelevance(int relevance) {
		fRelevance= relevance;
	}
}
