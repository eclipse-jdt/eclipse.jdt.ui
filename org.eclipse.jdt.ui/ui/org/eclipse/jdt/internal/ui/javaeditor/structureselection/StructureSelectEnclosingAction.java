package org.eclipse.jdt.internal.ui.javaeditor.structureselection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditorMessages;

public class StructureSelectEnclosingAction extends StructureSelectionAction {

	public StructureSelectEnclosingAction(CompilationUnitEditor editor, SelectionHistory history) {
		super(JavaEditorMessages.getString("StructureSelectEnclosing.label"), editor, history);
		setToolTipText(JavaEditorMessages.getString("StructureSelectEnclosing.tooltip"));
		setDescription(JavaEditorMessages.getString("StructureSelectEnclosing.description"));
	}
	
	/*
	 * This constructor is for testing purpose only.
	 */
	public StructureSelectEnclosingAction() {
	}
		
	ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu, StructureSelectionAnalyzer selAnalyzer) throws JavaModelException{
		ASTNode first= selAnalyzer.getFirstSelectedNode();	
		if (first == null || first.getParent() == null) 
			return getLastCoveringNodeRange(oldSourceRange, cu, selAnalyzer);
			
		return getSelectedNodeSourceRange(cu, first.getParent());
	}
}
