package org.eclipse.jdt.internal.ui.refactoring.actions.structureselection;

import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;

class StructureSelectionAnalyzer extends SelectionAnalyzer{

	private ASTNode fLastCoveringNode;
	
	public StructureSelectionAnalyzer(Selection selection, boolean traverseSelectedNode) {
		super(selection, traverseSelectedNode);
	}

	protected boolean visitNode(ASTNode node) {
		if (getSelection().liesOutside(node))
			return super.visitNode(node);		
		if (getSelection().covers(node))
			return super.visitNode(node);
		if (! getSelection().coveredBy(node))
			return super.visitNode(node);
		
		fLastCoveringNode= node;
		return true;
	}
	
	public ASTNode getLastCoveringNode() {
		return fLastCoveringNode;
	}
}
