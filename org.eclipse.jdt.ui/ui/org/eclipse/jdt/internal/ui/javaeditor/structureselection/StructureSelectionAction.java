package org.eclipse.jdt.internal.ui.javaeditor.structureselection;

import java.util.Collection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

public abstract class StructureSelectionAction extends Action {

	public static final String NEXT= "SelectNextElement"; //$NON-NLS-1$
	public static final String PREVIOUS= "SelectPreviousElement"; //$NON-NLS-1$
	public static final String ENCLOSING= "SelectEnclosingElement"; //$NON-NLS-1$
	public static final String HISTORY= "RestoreLastSelection"; //$NON-NLS-1$

	private CompilationUnitEditor fEditor;
	private SelectionHistory fSelectionHistory;

	protected StructureSelectionAction(String text, CompilationUnitEditor editor, SelectionHistory history) {
		super(text);
		Assert.isNotNull(editor);
		Assert.isNotNull(history);
		fEditor= editor;
		fSelectionHistory= history;
		setEnabled(null != SelectionConverter.getInputAsCompilationUnit(fEditor));
	}
	
	/*
	 * This constructor is for testing purpose only.
	 */
	protected StructureSelectionAction() {
		super(""); //$NON-NLS-1$
	}
	
	/* (non-JavaDoc)
	 * Method declared in IAction.
	 */
	public final  void run() {
		ITextSelection selection= getTextSelection();
		ISourceRange newRange= getNewSelectionRange(createSourceRange(selection), getCompilationUnit());
		// Check if new selection differs from current selection
		if (selection.getOffset() == newRange.getOffset() && selection.getLength() == newRange.getLength())
			return;
		fSelectionHistory.remember(new SourceRange(selection.getOffset(), selection.getLength()));
		try {
			fSelectionHistory.ignoreSelectionChanges();
			fEditor.selectAndReveal(newRange.getOffset(), newRange.getLength());
		} finally {
			fSelectionHistory.listenToSelectionChanges();
		}
	}
	
	public final ISourceRange getNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu){
		try{
			if (! cu.isStructureKnown())
				return oldSourceRange;
			CompilationUnit root= AST.parseCompilationUnit(cu, false);
			Selection selection= Selection.createFromStartLength(oldSourceRange.getOffset(), oldSourceRange.getLength());
			SelectionAnalyzer selAnalyzer= new SelectionAnalyzer(selection, true);
			root.accept(selAnalyzer);
			return internalGetNewSelectionRange(oldSourceRange, cu, selAnalyzer);
	 	}	catch (JavaModelException e){
	 		JavaPlugin.log(e); //dialog would be too heavy here
	 		return new SourceRange(oldSourceRange.getOffset(), oldSourceRange.getLength());
	 	}
	}
	
	/**
	 * This is the default implementation - it goes up one level in the AST.
	 * Subclasses may implement different behavior and/or use this implementation as a fallback for cases they do not handle..
	 */
	abstract ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu, SelectionAnalyzer selAnalyzer) throws JavaModelException;
	
	protected final ICompilationUnit getCompilationUnit() {
		return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
	}
	
	protected final ITextSelection getTextSelection() {
		return (ITextSelection)fEditor.getSelectionProvider().getSelection();
	}
	
	protected static ISourceRange getLastCoveringNodeRange(ISourceRange oldSourceRange, ICompilationUnit cu, SelectionAnalyzer selAnalyzer) throws JavaModelException {
		if (selAnalyzer.getLastCoveringNode() == null)
			return oldSourceRange;		
		else	
			return getSelectedNodeSourceRange(cu, selAnalyzer.getLastCoveringNode());
	}
	
	protected static ISourceRange getSelectedNodeSourceRange(ICompilationUnit cu, ASTNode nodeToSelect) throws JavaModelException {
		int offset= nodeToSelect.getStartPosition();
		int end= Math.min(cu.getSourceRange().getLength(), nodeToSelect.getStartPosition() + nodeToSelect.getLength() - 1);
		return createSourceRange(offset, end);
	}
	
	//-- private helper methods
	
	private static ISourceRange createSourceRange(ITextSelection ts){
		return new SourceRange(ts.getOffset(), ts.getLength());
	}
	
	//-- helper methods for this class and subclasses
	
	static ISourceRange createSourceRange(int offset, int end){
		int length= end - offset + 1;
		if (length == 0) //to allow 0-length selection
			length= 1;
		return new SourceRange(Math.max(0, offset), length);
	}
	
	static ASTNode[] getChildNodes(ASTNode node){
		if (node instanceof Block)
			return convertToNodeArray(((Block)node).statements());	
		if (node instanceof TypeDeclaration)
			return convertToNodeArray(((TypeDeclaration)node).bodyDeclarations());	
		if (node instanceof CompilationUnit)
			return convertToNodeArray(((CompilationUnit)node).types());	
		return null;	
	}
	
	private static ASTNode[] convertToNodeArray(Collection statements){
		return (ASTNode[]) statements.toArray(new ASTNode[statements.size()]);
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
