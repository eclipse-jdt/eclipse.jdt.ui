package org.eclipse.jdt.internal.ui.refactoring.actions.structureselection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;

public class StructureSelectNextAction extends StructureSelectionAction{
	
	public StructureSelectNextAction() {
		super("Structure Select Next"); 
		setText("&Next Element@Alt+ARROW_RIGHT");
	}
	
	/* non java doc
	 * @see StructureSelectionAction#internalGetNewSelectionRange(ISourceRange, ICompilationUnit, StructureSelectionAnalyzer)
	 */
	ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu, StructureSelectionAnalyzer selAnalyzer) throws JavaModelException{
		ASTNode first= selAnalyzer.getFirstSelectedNode();
		if (first == null) 
			return super.internalGetNewSelectionRange(oldSourceRange, cu, selAnalyzer); 
		
		ASTNode parent= first.getParent();
		if (parent == null)	
			return super.internalGetNewSelectionRange(oldSourceRange, cu, selAnalyzer); 
		
		ASTNode lastSelectedNode= selAnalyzer.getSelectedNodes()[selAnalyzer.getSelectedNodes().length - 1];
		ASTNode nextNode= getNextNode(parent, lastSelectedNode);
		if (nextNode == parent)
			return super.internalGetNewSelectionRange(oldSourceRange, cu, selAnalyzer);
		int offset= oldSourceRange.getOffset();
		int end= Math.min(cu.getSourceRange().getLength(), nextNode.getStartPosition() + nextNode.getLength() - 1);
		return StructureSelectionAction.createSourceRange(offset, end);			
	}
	
	private static ASTNode getNextNode(ASTNode parent, ASTNode node){
		Statement[] statements= StructureSelectionAction.getStatements(parent);
		if (statements == null || statements.length == 0)
			return parent;
		if (node == statements[statements.length -1 ])
			return parent;
		else
			return statements[StructureSelectionAction.findIndex(statements, node) + 1];
	}
}

