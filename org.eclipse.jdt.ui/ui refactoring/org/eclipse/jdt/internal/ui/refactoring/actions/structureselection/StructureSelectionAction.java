package org.eclipse.jdt.internal.ui.refactoring.actions.structureselection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.actions.TextSelectionAction;

/**
 * Action used to structurally select a fragment of source code.
 * Run, this action will select the parent of the currently selected syntax tree node, or the node itself if it has been only
 * partially covered by the selection. 
 */
public class StructureSelectionAction extends TextSelectionAction {
	
	protected StructureSelectionAction(String text) {
		super(text);
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
			AST ast= new AST(cu);
			if (ast.isMalformed())
				return oldSourceRange;	
			StructureSelectionAnalyzer selAnalyzer= new StructureSelectionAnalyzer(cu.getBuffer(), oldSourceRange.getOffset(), oldSourceRange.getLength());
			ast.accept(selAnalyzer);
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
		AstNode[] parents= selAnalyzer.getParents();
		if (parents == null || parents.length == 0){
			if (selAnalyzer.getLastCoveringNode() != null)
				return getSelectedNodeSourceRange(cu, selAnalyzer.getLastCoveringNode());
			else
				return oldSourceRange;
		} 	else
				return getSelectedNodeSourceRange(cu, parents[parents.length - 1]);
	}
	
	//-- private helper methods
	
	private static ISourceRange getSelectedNodeSourceRange(ICompilationUnit cu, AstNode nodeToSelect) throws JavaModelException {
		int offset= ASTUtil.getSourceStart(nodeToSelect);
		int end= Math.min(cu.getSourceRange().getLength(), ASTUtil.getSourceEnd(nodeToSelect));
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
	
	static boolean containsStatements(AstNode parent) {
		if (parent instanceof Block) 
			return true;
			
		if (parent instanceof AbstractMethodDeclaration)
			return true;
			
		return false;	
	}
	
	static Statement[] getStatements(AstNode node){
		if (node instanceof Block)
			return ((Block)node).statements;
		if (node instanceof AbstractMethodDeclaration)
			return ((AbstractMethodDeclaration)node).statements;		
		Assert.isTrue(false);	
		return null;	
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

