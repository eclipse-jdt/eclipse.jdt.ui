/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.selectionactions;

import java.util.Collection;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IEditorInput;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditorMessages;

public abstract class StructureSelectionAction extends Action {

	public static final String NEXT= "SelectNextElement"; //$NON-NLS-1$
	public static final String PREVIOUS= "SelectPreviousElement"; //$NON-NLS-1$
	public static final String ENCLOSING= "SelectEnclosingElement"; //$NON-NLS-1$
	public static final String HISTORY= "RestoreLastSelection"; //$NON-NLS-1$

	private JavaEditor fEditor;
	private SelectionHistory fSelectionHistory;

	protected StructureSelectionAction(String text, JavaEditor editor, SelectionHistory history) {
		super(text);
		Assert.isNotNull(editor);
		Assert.isNotNull(history);
		fEditor= editor;
		fSelectionHistory= history;
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
		ISourceReference source= getSourceReference();
		ISourceRange sourceRange;
		try {
			sourceRange= source.getSourceRange();
			if (sourceRange == null || sourceRange.getLength() == 0) {
				MessageDialog.openInformation(fEditor.getEditorSite().getShell(), 
					JavaEditorMessages.getString("StructureSelect.error.title"),  //$NON-NLS-1$
					JavaEditorMessages.getString("StructureSelect.error.message")); //$NON-NLS-1$
				return;
			}
		} catch (JavaModelException e) {
		}
		ISourceRange newRange= getNewSelectionRange(createSourceRange(selection), source);
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
	
	public final ISourceRange getNewSelectionRange(ISourceRange oldSourceRange, ISourceReference sr) {
		try{
			if (! isStructureKnown(sr))
				return oldSourceRange;
			CompilationUnit root= getAST(sr);
			if (root == null)
				return oldSourceRange;
			Selection selection= Selection.createFromStartLength(oldSourceRange.getOffset(), oldSourceRange.getLength());
			SelectionAnalyzer selAnalyzer= new SelectionAnalyzer(selection, true);
			root.accept(selAnalyzer);
			return internalGetNewSelectionRange(oldSourceRange, sr, selAnalyzer);
	 	}	catch (JavaModelException e){
	 		JavaPlugin.log(e); //dialog would be too heavy here
	 		return new SourceRange(oldSourceRange.getOffset(), oldSourceRange.getLength());
	 	}
	}
	
	/**
	 * This is the default implementation - it goes up one level in the AST.
	 * Subclasses may implement different behavior and/or use this implementation as a fallback for cases they do not handle..
	 */
	abstract ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ISourceReference sr, SelectionAnalyzer selAnalyzer) throws JavaModelException;
	
	protected final ITextSelection getTextSelection() {
		return (ITextSelection)fEditor.getSelectionProvider().getSelection();
	}
	
	protected static ISourceRange getLastCoveringNodeRange(ISourceRange oldSourceRange, ISourceReference sr, SelectionAnalyzer selAnalyzer) throws JavaModelException {
		if (selAnalyzer.getLastCoveringNode() == null)
			return oldSourceRange;		
		else	
			return getSelectedNodeSourceRange(sr, selAnalyzer.getLastCoveringNode());
	}
	
	protected static ISourceRange getSelectedNodeSourceRange(ISourceReference sr, ASTNode nodeToSelect) throws JavaModelException {
		int offset= nodeToSelect.getStartPosition();
		int end= Math.min(sr.getSourceRange().getLength(), nodeToSelect.getStartPosition() + nodeToSelect.getLength() - 1);
		return createSourceRange(offset, end);
	}
	
	//-- private helper methods
	
	private static ISourceRange createSourceRange(ITextSelection ts){
		return new SourceRange(ts.getOffset(), ts.getLength());
	}
	
	private ISourceReference getSourceReference() {
		IEditorInput input= fEditor.getEditorInput();
		if (input instanceof IClassFileEditorInput) {
			return ((IClassFileEditorInput)input).getClassFile();
		} else {
			return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(input);
		}
	}
	
	private boolean isStructureKnown(ISourceReference sr) throws JavaModelException {
		if (sr instanceof ICompilationUnit)
			return ((ICompilationUnit)sr).isStructureKnown();
		else return true;
	}
	
	private CompilationUnit getAST(ISourceReference sr) {

		if (sr instanceof ICompilationUnit) {
			ASTParser p= ASTParser.newParser(AST.LEVEL_2_0);
			p.setSource((ICompilationUnit) sr);
			return (CompilationUnit) p.createAST(null);
		} else if (sr instanceof IClassFile) {
			ASTParser p= ASTParser.newParser(AST.LEVEL_2_0);
			p.setSource((IClassFile) sr);
			return (CompilationUnit) p.createAST(null);
		}
		return null;
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
