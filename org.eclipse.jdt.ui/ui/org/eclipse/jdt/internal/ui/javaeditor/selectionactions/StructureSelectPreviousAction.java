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

public class StructureSelectPreviousAction extends StructureSelectionAction{
	
	public StructureSelectPreviousAction(CompilationUnitEditor editor, SelectionHistory history) {
		super(JavaEditorMessages.getString("StructureSelectPrevious.label"), editor, history); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("StructureSelectPrevious.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("StructureSelectPrevious.description")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.STRUCTURED_SELECT_PREVIOUS_ACTION);
	}
	
	/*
	 * This constructor is for testing purpose only.
	 */
	public StructureSelectPreviousAction() {
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
		
		ASTNode previousNode= getPreviousNode(parent, selAnalyzer.getSelectedNodes()[0]);
		if (previousNode == parent)
			return getSelectedNodeSourceRange(cu, parent);	
			
		int offset= previousNode.getStartPosition();
		int end= oldSourceRange.getOffset() + oldSourceRange.getLength() - 1;
		return StructureSelectionAction.createSourceRange(offset, end);
	}
	
	private static ASTNode getPreviousNode(ASTNode parent, ASTNode node){
		ASTNode[] siblingNodes= StructureSelectionAction.getChildNodes(parent);
		if (siblingNodes == null || siblingNodes.length == 0)
			return parent;
		if (node == siblingNodes[0])
			return parent;
		else
			return siblingNodes[StructureSelectionAction.findIndex(siblingNodes, node) - 1];
	}
}

