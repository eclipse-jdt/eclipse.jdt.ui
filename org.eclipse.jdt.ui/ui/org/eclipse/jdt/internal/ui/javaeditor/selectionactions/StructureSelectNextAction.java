package org.eclipse.jdt.internal.ui.javaeditor.selectionactions;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditorMessages;

import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;

public class StructureSelectNextAction extends StructureSelectionAction{
	
	public StructureSelectNextAction(CompilationUnitEditor editor, SelectionHistory history) {
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
	ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu, SelectionAnalyzer selAnalyzer) throws JavaModelException{
		ASTNode first= selAnalyzer.getFirstSelectedNode();
		if (first == null) 
			return getLastCoveringNodeRange(oldSourceRange, cu, selAnalyzer); 
		
		ASTNode parent= first.getParent();
		if (parent == null)	
			return getLastCoveringNodeRange(oldSourceRange, cu, selAnalyzer); 
		
		ASTNode lastSelectedNode= selAnalyzer.getSelectedNodes()[selAnalyzer.getSelectedNodes().length - 1];
		ASTNode nextNode= getNextNode(parent, lastSelectedNode);
		if (nextNode == parent)
			return getSelectedNodeSourceRange(cu, first.getParent());
		int offset= oldSourceRange.getOffset();
		int end= Math.min(cu.getSourceRange().getLength(), nextNode.getStartPosition() + nextNode.getLength() - 1);
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

