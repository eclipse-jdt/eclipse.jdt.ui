/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.selectionactions;

import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditorMessages;

public class StructureSelectNextAction extends StructureSelectionAction{
	
	private static class NextNodeAnalyzer extends GenericVisitor {
		private final int fOffset;
		private ASTNode fNextNode;
		private NextNodeAnalyzer(int offset) {
			super(true);
			fOffset= offset;
		}
		public static ASTNode perform(int offset, ASTNode lastCoveringNode) {
			NextNodeAnalyzer analyzer= new NextNodeAnalyzer(offset);
			lastCoveringNode.accept(analyzer);
			return analyzer.fNextNode;
		}
		protected boolean visitNode(ASTNode node) {
			int start= node.getStartPosition();
			int end= start + node.getLength();
			if (start == fOffset) {
				fNextNode= node;
				return true;
			} else {
				return (start < fOffset && fOffset < end);
			}
		}
	}
	
	public StructureSelectNextAction(JavaEditor editor, SelectionHistory history) {
		super(JavaEditorMessages.getString("StructureSelectNext.label"), editor, history); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("StructureSelectNext.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("StructureSelectNext.description")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.STRUCTURED_SELECT_NEXT_ACTION);
	}
	
	/*
	 * This constructor is for testing purpose only.
	 */
	public StructureSelectNextAction() {
	}
		
	/* non java doc
	 * @see StructureSelectionAction#internalGetNewSelectionRange(ISourceRange, ICompilationUnit, SelectionAnalyzer)
	 */
	ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ISourceReference sr, SelectionAnalyzer selAnalyzer) throws JavaModelException{
		if (oldSourceRange.getLength() == 0 && selAnalyzer.getLastCoveringNode() != null) {
			ASTNode previousNode= NextNodeAnalyzer.perform(oldSourceRange.getOffset(), selAnalyzer.getLastCoveringNode());
			if (previousNode != null)
				return getSelectedNodeSourceRange(sr, previousNode);
		}
		ASTNode first= selAnalyzer.getFirstSelectedNode();
		if (first == null) 
			return getLastCoveringNodeRange(oldSourceRange, sr, selAnalyzer); 
		
		ASTNode parent= first.getParent();
		if (parent == null)	
			return getLastCoveringNodeRange(oldSourceRange, sr, selAnalyzer); 
		
		ASTNode lastSelectedNode= selAnalyzer.getSelectedNodes()[selAnalyzer.getSelectedNodes().length - 1];
		ASTNode nextNode= getNextNode(parent, lastSelectedNode);
		if (nextNode == parent)
			return getSelectedNodeSourceRange(sr, first.getParent());
		int offset= oldSourceRange.getOffset();
		int end= Math.min(sr.getSourceRange().getLength(), nextNode.getStartPosition() + nextNode.getLength() - 1);
		return StructureSelectionAction.createSourceRange(offset, end);			
	}
	
	private static ASTNode getNextNode(ASTNode parent, ASTNode node){
		ASTNode[] siblingNodes= StructureSelectionAction.getChildNodes(parent);
		if (siblingNodes == null || siblingNodes.length == 0)
			return parent;
		if (node == siblingNodes[siblingNodes.length -1 ])
			return parent;
		else
			return siblingNodes[StructureSelectionAction.findIndex(siblingNodes, node) + 1];
	}
}

