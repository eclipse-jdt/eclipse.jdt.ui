package org.eclipse.jdt.internal.ui.refactoring.actions.structureselection;

import org.eclipse.jdt.core.IBuffer;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.util.NewSelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;

public class StructureSelectionAnalyzer extends NewSelectionAnalyzer {

	private AstNode fLastCoveringNode;

	public StructureSelectionAnalyzer(IBuffer buffer, int start, int length) {
		super(new ExtendedBuffer(buffer), Selection.createFromStartLength(start, length));
	}
	
	protected boolean visitRange(int start, int end, AstNode node, Scope scope) {
		if (	!(end < fSelection.start || fSelection.end < start) 
		  && !(fSelection.covers(start, end))
		  && fSelection.coveredBy(start, end)) {
		  	
			handleSelectionCoveredByNode(node);
			return true;
		} else {
			return super.visitRange(start, end, node, scope);
		}	
	}

	public AstNode getLastCoveringNode() {
		return fLastCoveringNode;
	}

	private void handleSelectionCoveredByNode(AstNode node){
		fLastCoveringNode= node;
	}
}