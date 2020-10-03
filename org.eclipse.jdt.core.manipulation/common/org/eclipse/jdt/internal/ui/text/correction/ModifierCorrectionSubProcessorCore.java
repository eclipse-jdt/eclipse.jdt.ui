/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *     Benjamin Muskalla <bmuskalla@innoopract.com> - [quick fix] 'Remove invalid modifiers' does not appear for enums and annotations - https://bugs.eclipse.org/bugs/show_bug.cgi?id=110589
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [quick fix] Quick fix for missing synchronized modifier - https://bugs.eclipse.org/bugs/show_bug.cgi?id=245250
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *     Microsoft Corporation - extracted from ModifierCorrectionSubProcessor
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroupCore;

/**
  */
public class ModifierCorrectionSubProcessorCore {

	public static final String KEY_MODIFIER= "modifier"; //$NON-NLS-1$

	private static class ModifierLinkedModeProposal extends LinkedProposalPositionGroupCore.ProposalCore {

		private final int fModifier;

		public ModifierLinkedModeProposal(int modifier, int relevance) {
			super(null, relevance);
			fModifier= modifier;
		}

		@Override
		public String getAdditionalProposalInfo() {
			return getDisplayString();
		}

		@Override
		public String getDisplayString() {
			if (fModifier == 0) {
				return CorrectionMessages.ModifierCorrectionSubProcessor_default_visibility_label;
			} else {
				return ModifierKeyword.fromFlagValue(fModifier).toString();
			}
		}

		@Override
		public TextEdit computeEdits(int offset, LinkedPosition currentPosition, char trigger, int stateMask, LinkedModeModel model) throws CoreException {
			try {
				IDocument document= currentPosition.getDocument();
				MultiTextEdit edit= new MultiTextEdit();
				int documentLen= document.getLength();
				if (fModifier == 0) {
					int end= currentPosition.offset + currentPosition.length; // current end position
					int k= end;
					while (k < documentLen && IndentManipulation.isIndentChar(document.getChar(k))) {
						k++;
					}
					// first remove space then replace range (remove space can destroy empty position)
					edit.addChild(new ReplaceEdit(end, k - end, "")); // remove extra spaces //$NON-NLS-1$
					edit.addChild(new ReplaceEdit(currentPosition.offset, currentPosition.length, "")); //$NON-NLS-1$
				} else {
					// first then replace range the insert space (insert space can destroy empty position)
					edit.addChild(new ReplaceEdit(currentPosition.offset, currentPosition.length, ModifierKeyword.fromFlagValue(fModifier).toString()));
					int end= currentPosition.offset + currentPosition.length; // current end position
					if (end < documentLen && !Character.isWhitespace(document.getChar(end))) {
						edit.addChild(new ReplaceEdit(end, 0, String.valueOf(' '))); // insert extra space
					}
				}
				return edit;
			} catch (BadLocationException e) {
				throw new CoreException(new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, IStatus.ERROR, e.getMessage(), e));
			}
		}
	}

	public static void installLinkedVisibilityProposals(LinkedProposalModelCore linkedProposalModel, ASTRewrite rewrite, List<IExtendedModifier> modifiers, boolean inInterface, String groupId) {
		ASTNode modifier= findVisibilityModifier(modifiers);
		if (modifier != null) {
			int selected= ((Modifier) modifier).getKeyword().toFlagValue();

			LinkedProposalPositionGroupCore positionGroup= linkedProposalModel.getPositionGroup(groupId, true);
			positionGroup.addPosition(rewrite.track(modifier), false);
			positionGroup.addProposal(new ModifierLinkedModeProposal(selected, 10));

			// add all others
			int[] flagValues= inInterface ? new int[] { Modifier.PUBLIC, 0 } : new int[] { Modifier.PUBLIC, 0, Modifier.PROTECTED, Modifier.PRIVATE };
			for (int i= 0; i < flagValues.length; i++) {
				if (flagValues[i] != selected) {
					positionGroup.addProposal(new ModifierLinkedModeProposal(flagValues[i], 9 - i));
				}
			}
		}
	}

	public static void installLinkedVisibilityProposals(LinkedProposalModelCore linkedProposalModel, ASTRewrite rewrite, List<IExtendedModifier> modifiers, boolean inInterface) {
		ModifierCorrectionSubProcessorCore.installLinkedVisibilityProposals(linkedProposalModel, rewrite, modifiers, inInterface, KEY_MODIFIER);
	}

	private static Modifier findVisibilityModifier(List<IExtendedModifier> modifiers) {
		for (IExtendedModifier curr : modifiers) {
			if (curr instanceof Modifier) {
				Modifier modifier= (Modifier) curr;
				ModifierKeyword keyword= modifier.getKeyword();
				if (keyword == ModifierKeyword.PUBLIC_KEYWORD || keyword == ModifierKeyword.PROTECTED_KEYWORD || keyword == ModifierKeyword.PRIVATE_KEYWORD) {
					return modifier;
				}
			}
		}
		return null;
	}

	private ModifierCorrectionSubProcessorCore() {
	}
}
