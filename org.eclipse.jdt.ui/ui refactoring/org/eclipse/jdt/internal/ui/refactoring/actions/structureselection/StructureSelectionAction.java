package org.eclipse.jdt.internal.ui.refactoring.actions.structureselection;

import java.util.Collection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.actions.TextSelectionAction;

public class StructureSelectionAction extends TextSelectionAction {

	protected StructureSelectionAction(String text) {
		super(text);
		setText("&Enclosing Element@Alt+ARROW_UP");
	}
	
	public StructureSelectionAction() {
		this("Structure Select");
	}

	/* (non-JavaDoc)
	 * Method declared in IAction.
	 */
	public final  void run() {
		ISourceRange newRange= getNewSelectionRange(createSourceRange(getTextSelection()), getCompilationUnit());
		getEditor().selectAndReveal(newRange.getOffset(), newRange.getLength());
	}
	
	public final ISourceRange getNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu){
		try{
			if (! cu.isStructureKnown())
				return oldSourceRange;
			CompilationUnit root= AST.parseCompilationUnit(cu, false);
			Selection selection= Selection.createFromStartLength(oldSourceRange.getOffset(), oldSourceRange.getLength());
			StructureSelectionAnalyzer selAnalyzer= new StructureSelectionAnalyzer(selection, true);
			root.accept(selAnalyzer);
			return internalGetNewSelectionRange(oldSourceRange, cu, selAnalyzer);
	 	}	catch (JavaModelException e){
	 		JavaPlugin.log(e); //dialog would be too heavy here
	 		return new SourceRange(oldSourceRange.getOffset(), oldSourceRange.getLength());
	 	}
	}
	
	protected boolean canOperateOnCurrentSelection(ISelection selection) {
		if (super.canOperateOnCurrentSelection(selection))
			return true;
		
		return selection instanceof ITextSelection;
	}
	
	/**
	 * This is the default implementation - it goes up one level in the AST.
	 * Subclasses may implement different behavior and/or use this implementation as a fallback for cases they do not handle..
	 */
	ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu, StructureSelectionAnalyzer selAnalyzer) throws JavaModelException{
		ASTNode first= selAnalyzer.getFirstSelectedNode();
		if (first == null) {
			if (selAnalyzer.getLastCoveringNode() != null)
				return getSelectedNodeSourceRange(cu, selAnalyzer.getLastCoveringNode());
			else
				return oldSourceRange;		
		}	
		ASTNode parent= first.getParent();
		if (parent == null){
			if (selAnalyzer.getLastCoveringNode() != null)
				return getSelectedNodeSourceRange(cu, selAnalyzer.getLastCoveringNode());
			else
				return oldSourceRange;		
		}	
		return getSelectedNodeSourceRange(cu, parent);
	}
	
	//-- private helper methods
	
	private static ISourceRange getSelectedNodeSourceRange(ICompilationUnit cu, ASTNode nodeToSelect) throws JavaModelException {
		int offset= nodeToSelect.getStartPosition();
		int end= Math.min(cu.getSourceRange().getLength(), nodeToSelect.getStartPosition() + nodeToSelect.getLength() - 1);
		return createSourceRange(offset, end);
	}
	
	private static ISourceRange createSourceRange(ITextSelection ts){
		if (ts.getLength() == 0) //to allow 0-length selection
			return new SourceRange(ts.getOffset(), 1);
		else	
			return new SourceRange(ts.getOffset(), ts.getLength());
	}
	
	//-- helper methods for this class and subclasses
	
	static ISourceRange createSourceRange(int offset, int end){
		int length= end - offset + 1;
		if (length == 0) //to allow 0-length selection
			length= 1;
		return new SourceRange(Math.max(0, offset), length);
	}
	
	static Statement[] getStatements(ASTNode node){
		if (node instanceof Block)
			return convertToStatementArray(((Block)node).statements());	
			
		if (node instanceof MethodDeclaration){
			Block body= ((MethodDeclaration)node).getBody();
			if (body != null)
				return convertToStatementArray(body.statements());
			else
				return null;			
		}	
		return null;	
	}
	
	private static Statement[] convertToStatementArray(Collection statements){
		return (Statement[]) statements.toArray(new Statement[statements.size()]);
	}

	static int findIndex(Object[] array, Object o){
		for (int i= 0; i < array.length; i++) {
			Object object= array[i];
			if (object == o)
				return i;
		}
		return -1;
	}	

}
