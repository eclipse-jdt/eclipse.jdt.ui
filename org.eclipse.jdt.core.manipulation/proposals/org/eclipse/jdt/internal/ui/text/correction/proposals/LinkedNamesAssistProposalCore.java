/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;

import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;


/**
 * A template proposal.
 */
public class LinkedNamesAssistProposalCore extends CUCorrectionProposalCore {


	public static final String ASSIST_ID= "org.eclipse.jdt.ui.correction.renameInFile.assist"; //$NON-NLS-1$

	private SimpleName fNode;
	private IInvocationContext fContext;
	private String fLabel;
	private String fValueSuggestion;

	public LinkedNamesAssistProposalCore(IInvocationContext context, SimpleName node) {
		this(CorrectionMessages.LinkedNamesAssistProposal_description, context, node, null);
	}

	public LinkedNamesAssistProposalCore(String label, IInvocationContext context, SimpleName node, String valueSuggestion) {
		super(label, context.getCompilationUnit(), IProposalRelevance.LINKED_NAMES_ASSIST);
		fLabel= label;
		fNode= node;
		fContext= context;
		fValueSuggestion= valueSuggestion;
	}

	public String getLabel() {
		return fLabel;
	}

	public SimpleName getNode() {
		return fNode;
	}

	public IInvocationContext getContext() {
		return fContext;
	}

	public String getValueSuggestion() {
		return fValueSuggestion;
	}

	@Override
	public void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
		try {
//			Point seletion= viewer.getSelectedRange();

			// get full ast
			CompilationUnit root= SharedASTProviderCore.getAST(fContext.getCompilationUnit(), SharedASTProviderCore.WAIT_YES, null);

			ASTNode nameNode= NodeFinder.perform(root, fNode.getStartPosition(), fNode.getLength());
			final int pos= fNode.getStartPosition();

			ASTNode[] sameNodes;
			if (nameNode instanceof SimpleName) {
				sameNodes= LinkedNodeFinder.findByNode(root, (SimpleName) nameNode);
			} else {
				sameNodes= new ASTNode[] { nameNode };
			}

			// sort for iteration order, starting with the node @ offset
			Arrays.sort(sameNodes, new Comparator<ASTNode>() {

				@Override
				public int compare(ASTNode o1, ASTNode o2) {
					return rank(o1) - rank(o2);
				}

				/**
				 * Returns the absolute rank of an <code>ASTNode</code>. Nodes
				 * preceding <code>offset</code> are ranked last.
				 *
				 * @param node the node to compute the rank for
				 * @return the rank of the node with respect to the invocation offset
				 */
				private int rank(ASTNode node) {
					int relativeRank= node.getStartPosition() + node.getLength() - pos;
					if (relativeRank < 0)
						return Integer.MAX_VALUE + relativeRank;
					else
						return relativeRank;
				}

			});

			LinkedPositionGroup group= new LinkedPositionGroup();
			for (int i= 0; i < sameNodes.length; i++) {
				ASTNode elem= sameNodes[i];
				group.addPosition(new LinkedPosition(document, elem.getStartPosition(), elem.getLength(), i));
			}

			LinkedModeModel model= new LinkedModeModel();
			model.addGroup(group);
			model.forceInstall();

			if (fValueSuggestion != null) {
				ReplaceEdit edit= new ReplaceEdit(nameNode.getStartPosition(), nameNode.getLength(), fValueSuggestion);
				for (ASTNode sameNode : sameNodes) {
					edit= new ReplaceEdit(sameNode.getStartPosition(), sameNode.getLength(), fValueSuggestion);
					rootEdit.addChild(edit);
				}
//				document.replace(nameNode.getStartPosition(), nameNode.getLength(), fValueSuggestion);
			}
		} catch (BadLocationException e) {
			JavaManipulationPlugin.log(e);
		}
	}

	@Override
	public String getCommandId() {
		return ASSIST_ID;
	}

}
