package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.TextOperationAction;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.actions.ExtractMethodAction;
import org.eclipse.jdt.internal.ui.reorg.CUSavePolicy;

/**
 * Java specific text editor.
 */
public class CompilationUnitEditor extends JavaEditor {
	
	/** Save policy determining the compilation unit save behavior */
	protected ISavePolicy fSavePolicy;
	
	/**
	 * Default constructor.
	 */
	public CompilationUnitEditor() {
		super();
		setDocumentProvider(JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
		setEditorContextMenuId("#CompilationUnitEditorContext");
		setRulerContextMenuId("#CompilationUnitRulerContext");
		setOutlinerContextMenuId("#CompilationUnitOutlinerContext");
		fSavePolicy= new CUSavePolicy();
	}
	
	/**
	 * @see AbstractTextEditor#createActions
	 */
	protected void createActions() {
		
		super.createActions();
		
		setAction("ContentAssistProposal", new TextOperationAction(getResourceBundle(), "Editor.ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS));			
		setAction("AddImportOnSelection", new AddImportOnSelectionAction(this));		
		setAction("OrganizeImports", new OrganizeImportsAction(this));
		
		setAction("Comment", new TextOperationAction(getResourceBundle(), "Editor.Comment.", this, ITextOperationTarget.PREFIX));
		setAction("Uncomment", new TextOperationAction(getResourceBundle(), "Editor.Uncomment.", this, ITextOperationTarget.STRIP_PREFIX));
		// setAction("Format", new TextOperationAction(getResourceBundle(), "Editor.Format.", this, ISourceViewer.FORMAT));
		
		setAction("AddBreakpoint", new AddBreakpointAction(getResourceBundle(), "Editor.AddBreakpoint.", this));
		setAction("ManageBreakpoints", new BreakpointRulerAction(getResourceBundle(), "Editor.ManageBreakpoints.", getVerticalRuler(), this));
		
		setAction("ExtractMethod", new ExtractMethodAction(this));
		
		setAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK, getAction("ManageBreakpoints"));		
	}
	
	/**
	 * @see JavaEditor#getJavaSourceReferenceAt
	 */
	protected ISourceReference getJavaSourceReferenceAt(int position) {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
		if (unit != null) {
			try {
				IJavaElement element= unit.getElementAt(position);
				if (element instanceof ISourceReference)
					return (ISourceReference) element;
			} catch (JavaModelException x) {
			}
		}
		return null;
	}
	
	/**
	 * @see AbstractEditor#editorContextMenuAboutToChange
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, IContextMenuConstants.GROUP_REORGANIZE, "ExtractMethod");
		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "ContentAssistProposal");
		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "AddImportOnSelection");
		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "OrganizeImports");
		addAction(menu, ITextEditorActionConstants.GROUP_ADD, "AddBreakpoint");
		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, "Comment");
		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, "Uncomment");
		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, "Format");
	}
	
	/**
	 * @see AbstractTextEditor#rulerContextMenuAboutToShow
	 */
	protected void rulerContextMenuAboutToShow(IMenuManager menu) {
		super.rulerContextMenuAboutToShow(menu);
		addAction(menu, "ManageBreakpoints");
	}
	
	/**
	 * @see JavaEditor#createOutlinePage
	 */
	protected JavaOutlinePage createOutlinePage() {
		JavaOutlinePage page= super.createOutlinePage();
		page.setAction("DeleteElement", new DeleteISourceManipulationsAction(getResourceBundle(), "Outliner.DeleteISourceManipulations.", page));
		page.setAction("OrganizeImports", new OrganizeImportsAction(this));
		return page;
	}

	/**
	 * @see JavaEditor#setOutlinePageInput(JavaOutlinePage, IEditorInput)
	 */
	protected void setOutlinePageInput(JavaOutlinePage page, IEditorInput input) {
		if (page != null) {
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
			page.setInput(manager.getWorkingCopy(input));
		}
	}
	
	/**
	 * @see AbstarctTextEditor#doSave(IProgressMonitor)
	 */
	public void doSave(IProgressMonitor progressMonitor) {
		
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
		
		synchronized (unit) {
			
			ICompilationUnit original= (ICompilationUnit) unit.getOriginalElement();
			
			if (fSavePolicy != null)
				fSavePolicy.preSave(original);
			
			super.doSave(progressMonitor);
			
			if (fSavePolicy != null)
				fSavePolicy.postSave(original);
		}
	}	
}