/*******************************************************************************
 * Copyright (c) 2021 Gayan Perera and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class JavaLambdaCompletionProposal extends LazyJavaCompletionProposal {
	private int[] fArgumentLengths;

	private int[] fArgumentOffsets;

	private IRegion fSelectedRegion;


	public JavaLambdaCompletionProposal(CompletionProposal proposal, JavaContentAssistInvocationContext context) {
		super(proposal, context);
	}

	protected boolean needsLinkedMode() {
		return getParameterCount() > 0;
	}

	private int getParameterCount() {
		return Signature.getParameterCount(this.fProposal.getSignature());
	}

	@Override
	protected String computeReplacementString() {
		StringBuilder buffer= new StringBuilder();
		FormatterPrefs prefs= getFormatterPrefs();

		final int parameterCount= getParameterCount();
		this.fArgumentLengths= new int[parameterCount];
		this.fArgumentOffsets= new int[parameterCount];

		if (parameterCount > 1 || parameterCount == 0) {
			buffer.append(LPAREN);
		}

		if (parameterCount > 0) {
			appendParameterNames(buffer, prefs);
		}

		if (parameterCount > 1 || parameterCount == 0) {
			buffer.append(RPAREN);
		}
		buffer.append(SPACE);
		buffer.append(this.fProposal.getCompletion());
		buffer.append(SPACE);
		return buffer.toString();
	}

	private void appendParameterNames(StringBuilder buffer, FormatterPrefs prefs) {
		char[][] parameterNames= this.fProposal.findParameterNames(null);

		for (int i= 0; i < parameterNames.length; i++) {
			char[] pname= parameterNames[i];

			if (i != 0) {
				if (prefs.beforeComma) {
					buffer.append(SPACE);
				}
				buffer.append(COMMA);
				if (prefs.afterComma) {
					buffer.append(SPACE);
				}
			}
			this.fArgumentOffsets[i]= buffer.length();
			buffer.append(pname);
			this.fArgumentLengths[i]= pname.length;
		}
	}

	@Override
	public void apply(IDocument document, char trigger, int offset) {
		super.apply(document, trigger, offset);
		int baseOffset= getReplacementOffset();
		String replacement= getReplacementString();

		if (fArgumentOffsets != null && fArgumentOffsets.length > 0 && getTextViewer() != null) {
			try {
				LinkedModeModel model= new LinkedModeModel();
				for (int i= 0; i != fArgumentOffsets.length; i++) {
					LinkedPositionGroup group= new LinkedPositionGroup();
					group.addPosition(new LinkedPosition(document, baseOffset + fArgumentOffsets[i], fArgumentLengths[i], LinkedPositionGroup.NO_STOP));
					model.addGroup(group);
				}

				model.forceInstall();
				JavaEditor editor= EditorUtility.getActiveJavaEditor();
				if (editor != null) {
					model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
				}

				LinkedModeUI ui= new EditorLinkedModeUI(model, getTextViewer());
				ui.setExitPosition(getTextViewer(), baseOffset + replacement.length(), 0, Integer.MAX_VALUE);
				ui.setExitPolicy(new ExitPolicy('}', document));
				ui.setDoContextInfo(true);
				ui.setCyclingMode(LinkedModeUI.CYCLE_WHEN_NO_PARENT);
				ui.enter();

				fSelectedRegion= ui.getSelectedRegion();

			} catch (BadLocationException e) {
				JavaPlugin.log(e);
				MessageDialog.openError(getTextViewer().getTextWidget().getShell(), JavaTextMessages.FilledArgumentNamesMethodProposal_error_msg,
						e.getMessage());
			}
		} else {
			fSelectedRegion= new Region(baseOffset + replacement.length(), 0);
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		if (fSelectedRegion == null)
			return new Point(getReplacementOffset(), 0);

		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}
}
