package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.IDocument;import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.tasklist.TaskList;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.compare.JavaAddElementFromHistory;
import org.eclipse.jdt.internal.ui.compare.JavaReplaceWithEditionAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.ExtractMethodAction;
import org.eclipse.jdt.internal.ui.reorg.CUSavePolicy;


/**
 * Java specific text editor.
 */
public class CompilationUnitEditor extends JavaEditor {
	
	
	/** Save policy determining the compilation unit save behavior */
	protected ISavePolicy fSavePolicy;
	
	/** The status line clearer */
	protected ISelectionChangedListener fStatusLineClearer;
	
	
	
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
		page.setAction("ReplaceWithEdition", new JavaReplaceWithEditionAction(page));
		page.setAction("AddEdition", new JavaAddElementFromHistory(this, page));
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
	 * @see AbstractTextEditor#doSave(IProgressMonitor)
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
		
		getStatusLineManager().setErrorMessage("");
	}
		
	public void gotoError(boolean forward) {
		
		ISelectionProvider provider= getSelectionProvider();
		
		if (fStatusLineClearer != null) {
			provider.removeSelectionChangedListener(fStatusLineClearer);
			fStatusLineClearer= null;
		}
		
		ITextSelection s= (ITextSelection) provider.getSelection();
		IMarker nextError= getNextError(s.getOffset(), forward);
		
		if (nextError != null) {
			
			gotoMarker(nextError);
			
			IWorkbenchPage page= getSite().getPage();
			
			IViewPart view= view= page.findView("org.eclipse.ui.views.TaskList");
			if (view instanceof TaskList) {
				StructuredSelection ss= new StructuredSelection(nextError);
				((TaskList) view).setSelection(ss, true);
			}
			
			getStatusLineManager().setErrorMessage(nextError.getAttribute(IMarker.MESSAGE, ""));
			fStatusLineClearer= new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					getSelectionProvider().removeSelectionChangedListener(fStatusLineClearer);
					fStatusLineClearer= null;
					getStatusLineManager().setErrorMessage("");
				}
			};
			provider.addSelectionChangedListener(fStatusLineClearer);
			
		} else {
			
			getStatusLineManager().setErrorMessage("");
			
		}
	}

	private IMarker getNextError(int offset, boolean forward) {
		
		IMarker nextError= null;
		
		IDocument document= getDocumentProvider().getDocument(getEditorInput());
		int endOfDocument= document.getLength(); 
		int distance= 0;
		
		IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());
		Iterator e= model.getAnnotationIterator();
		while (e.hasNext()) {
			Annotation a= (Annotation) e.next();
			if (a instanceof MarkerAnnotation) {
				MarkerAnnotation ma= (MarkerAnnotation) a;
				IMarker marker= ma.getMarker();
		
				if (MarkerUtilities.isMarkerType(marker, IMarker.PROBLEM)) {
					Position p= model.getPosition(a);
					if (!p.includes(offset)) {
						
						int currentDistance= 0;
						
						if (forward) {
							currentDistance= p.getOffset() - offset;
							if (currentDistance < 0)
								currentDistance= endOfDocument - offset + p.getOffset();
						} else {
							currentDistance= offset - p.getOffset();
							if (currentDistance < 0)
								currentDistance= offset + endOfDocument - p.getOffset();
						}						
												
						if (nextError == null || currentDistance < distance) {
							distance= currentDistance;
							nextError= marker;
						}

					}
				}
		
			}
		}
		
		return nextError;
	}
	
	public void doSaveAs() {
		
		Shell shell= getSite().getShell();
		
		SaveAsDialog dialog= new SaveAsDialog(shell);
		dialog.open();
		IPath path= dialog.getResult();
		
		if (path == null)
			return;
			
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IFile file= workspace.getRoot().getFile(path);
		final IEditorInput newInput= new FileEditorInput(file);
		
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(final IProgressMonitor monitor) throws CoreException {
				getDocumentProvider().saveDocument(monitor, newInput, getDocumentProvider().getDocument(getEditorInput()));
			}
		};
		
		boolean success= false;
		try {
			
			getDocumentProvider().aboutToChange(newInput);
			new ProgressMonitorDialog(shell).run(false, true, op);
			success= true;
			
		} catch (InterruptedException x) {
		} catch (InvocationTargetException x) {
			// String title= getResourceString("Error.save_as.title", "Error.save_as.title");
			// String msg= getResourceString("Error.save_as.message", "Error.save_as.message");
			// MessageDialog.openError(shell, title, msg + x.getTargetException().getMessage());
		} finally {
			getDocumentProvider().changed(newInput);
			if (success)
				setInput(newInput);
		}
	}
	
	public boolean isSaveAsAllowed() {
		return true;
	}
}