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

public class StructureSelectEnclosingAction extends StructureSelectionAction {

	public StructureSelectEnclosingAction(CompilationUnitEditor editor, SelectionHistory history) {
		super(JavaEditorMessages.getString("StructureSelectEnclosing.label"), editor, history); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("StructureSelectEnclosing.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("StructureSelectEnclosing.description")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.STRUCTURED_SELECT_ENCLOSING_ACTION);
	}
	
	/*
	 * This constructor is for testing purpose only.
	 */
	public StructureSelectEnclosingAction() {
	}
		
    /*
     * @see StructureSelectionAction#internalGetNewSelectionRange(ISourceRange, ICompilationUnit, SelectionAnalyzer)
     */
	ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu, SelectionAnalyzer selAnalyzer) throws JavaModelException{
		ASTNode first= selAnalyzer.getFirstSelectedNode();	
		if (first == null || first.getParent() == null) 
			return getLastCoveringNodeRange(oldSourceRange, cu, selAnalyzer);
			
		return getSelectedNodeSourceRange(cu, first.getParent());
	}
}
