package org.eclipse.jdt.internal.ui.javaeditor.structureselection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditorMessages;

public class StructureSelectPreviousAction extends StructureSelectionAction{
	
	public StructureSelectPreviousAction(CompilationUnitEditor editor, SelectionHistory history) {
		super(JavaEditorMessages.getString("StructureSelectPrevious.label"), editor, history);
		setToolTipText(JavaEditorMessages.getString("StructureSelectPrevious.tooltip"));
		setDescription(JavaEditorMessages.getString("StructureSelectPrevious.description"));
	}
	
	/*
	 * This constructor is for testing purpose only.
	 */
	public StructureSelectPreviousAction() {
	}
		
	/* non java doc
	 * @see StructureSelectionAction#internalGetNewSelectionRange(ISourceRange, ICompilationUnit, StructureSelectionAnalyzer)
	 */	
	ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu, StructureSelectionAnalyzer selAnalyzer) throws JavaModelException{
		ASTNode first= selAnalyzer.getFirstSelectedNode();
		if (first == null) 
			return getLastCoveringNodeRange(oldSourceRange, cu, selAnalyzer); 
		
		ASTNode parent= first.getParent();
		if (parent == null)	
			return getLastCoveringNodeRange(oldSourceRange, cu, selAnalyzer); 
		
		ASTNode previousNode= getPreviousNode(parent, selAnalyzer.getSelectedNodes()[0]);
		if (previousNode == parent)
			return getSelectedNodeSourceRange(cu, parent);	
			
		int offset= previousNode.getStartPosition();
		int end= oldSourceRange.getOffset() + oldSourceRange.getLength() - 1;
		return StructureSelectionAction.createSourceRange(offset, end);
	}
	
	private static ASTNode getPreviousNode(ASTNode parent, ASTNode node){
		Statement[] statements= StructureSelectionAction.getStatements(parent);
		if (statements == null || statements.length == 0)
			return parent;
		if (node == statements[0])
			return parent;
		else
			return statements[StructureSelectionAction.findIndex(statements, node) - 1];
	}
}

