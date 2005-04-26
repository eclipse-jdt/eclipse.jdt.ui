/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * An experimental proposal.
 */
public final class ExperimentalProposal extends JavaMethodCompletionProposal {

	private char[][] fParameterNames;
	private String fName; // initialized by apply()

	private IRegion fSelectedRegion; // initialized by apply()

	public ExperimentalProposal(CompletionProposal proposal, ICompilationUnit cu) {
		super(proposal, cu);
	}

	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		int baseOffset= getReplacementOffset();

		String replacementString;
		int parameterCount;
		int[] positionOffsets, positionLengths;
		if (appendArguments(document, offset)) {
			fParameterNames= fProposal.findParameterNames(null);
			fName= String.valueOf(fProposal.getName());
			parameterCount= fParameterNames.length;
			positionOffsets= new int[parameterCount];
			positionLengths= new int[parameterCount];
			replacementString= computeReplacementString(baseOffset, positionOffsets, positionLengths, document);
		} else {
			parameterCount= 0;
			positionOffsets= new int[0];
			positionLengths= new int[0];

			replacementString= getReplacementString();
		}

		setReplacementString(replacementString);

		super.apply(document, trigger, offset);

		if (positionOffsets.length > 0 && getTextViewer() != null) {
			try {
				LinkedModeModel model= new LinkedModeModel();
				for (int i= 0; i != positionOffsets.length; i++) {
					LinkedPositionGroup group= new LinkedPositionGroup();
					group.addPosition(new LinkedPosition(document, baseOffset + positionOffsets[i], positionLengths[i], LinkedPositionGroup.NO_STOP));
					model.addGroup(group);
				}

				model.forceInstall();
				JavaEditor editor= getJavaEditor();
				if (editor != null) {
					model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
				}

				LinkedModeUI ui= new EditorLinkedModeUI(model, getTextViewer());
				ui.setExitPosition(getTextViewer(), baseOffset + replacementString.length(), 0, Integer.MAX_VALUE);
				ui.setExitPolicy(new ExitPolicy(')', document));
				ui.setDoContextInfo(true);
				ui.enter();

				fSelectedRegion= ui.getSelectedRegion();

			} catch (BadLocationException e) {
				JavaPlugin.log(e);
				openErrorDialog(e);
			}
		} else
			fSelectedRegion= new Region(baseOffset + replacementString.length(), 0);
	}

	private String computeReplacementString(int baseOffset, int[] offsets, int[] lengths, IDocument document) {
		StringBuffer buffer= new StringBuffer(fName);
		int count= fParameterNames.length;

		buffer.append('(');
		for (int i= 0; i != count; i++) {
			if (i != 0)
				buffer.append(", "); //$NON-NLS-1$

			offsets[i]= buffer.length();
			buffer.append(fParameterNames[i]);
			lengths[i]= buffer.length() - offsets[i];
		}
		buffer.append(')');
		return buffer.toString();
	}

	/**
	 * Returns the currently active java editor, or <code>null</code> if it
	 * cannot be determined.
	 *
	 * @return  the currently active java editor, or <code>null</code>
	 */
	private JavaEditor getJavaEditor() {
		IEditorPart part= JavaPlugin.getActivePage().getActiveEditor();
		if (part instanceof JavaEditor)
			return (JavaEditor) part;
		else
			return null;
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fSelectedRegion == null)
			return new Point(getReplacementOffset(), 0);

		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	private void openErrorDialog(BadLocationException e) {
		Shell shell= getTextViewer().getTextWidget().getShell();
		MessageDialog.openError(shell, JavaTextMessages.ExperimentalProposal_error_msg, e.getMessage());
	}

	private boolean appendArguments(IDocument document, int offset) {

		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION) ^ isToggleEating())
			return true;

		try {
			IRegion region= document.getLineInformationOfOffset(offset);
			String line= document.get(region.getOffset(), region.getLength());

			int index= offset - region.getOffset();
			while (index != line.length() && Character.isUnicodeIdentifierPart(line.charAt(index)))
				++index;

			if (index == line.length())
				return true;

			return line.charAt(index) != '(';

		} catch (BadLocationException e) {
			return true;
		}
	}

}
