package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.jface.action.IMenuManager;

import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Java specific text editor.
 */
public class ClassFileEditor extends JavaEditor {
	
	/**
	 * Default constructor.
	 */
	public ClassFileEditor() {
		super();
		setDocumentProvider(JavaPlugin.getDefault().getClassFileDocumentProvider());
		setEditorContextMenuId("#ClassFileEditorContext");
		setRulerContextMenuId("#ClassFileRulerContext");
		setOutlinerContextMenuId("#ClassFileOutlinerContext");
	}
	
	/**
	 * @see AbstractTextEditor#createActions
	 */
	protected void createActions() {
		super.createActions();
		
		setAction(ITextEditorActionConstants.SAVE, null);
		setAction(ITextEditorActionConstants.REVERT_TO_SAVED, null);
				
		setAction("AddBreakpoint", new AddBreakpointAction(getResourceBundle(), "Editor.AddBreakpoint.", this));
		setAction("ManageBreakpoints", new BreakpointRulerAction(getResourceBundle(), "Editor.ManageBreakpoints.", getVerticalRuler(), this));
		
		// replace default action with class file specific ones
		setAction(ITextEditorActionConstants.BOOKMARK, new AddClassFileMarkerAction(getResourceBundle(), "Editor.AddBookmark.", this, IMarker.BOOKMARK, true));
		setAction(ITextEditorActionConstants.ADD_TASK, new AddClassFileMarkerAction(getResourceBundle(), "Editor.AddTask.", this, IMarker.TASK, false));
		setAction(ITextEditorActionConstants.RULER_MANAGE_BOOKMARKS, new ClassFileMarkerRulerAction(getResourceBundle(), "Editor.ManageBookmarks.", getVerticalRuler(), this, IMarker.BOOKMARK, true));
		setAction(ITextEditorActionConstants.RULER_MANAGE_TASKS, new ClassFileMarkerRulerAction(getResourceBundle(), "Editor.ManageTasks.", getVerticalRuler(), this, IMarker.TASK, true));

		setAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK, getAction("ManageBreakpoints"));		
	}
	
	/**
	 * @see JavaEditor#getJavaSourceReferenceAt
	 */
	protected ISourceReference getJavaSourceReferenceAt(int position) {
		if (getEditorInput() instanceof ClassFileEditorInput) {
			try {
				
				ClassFileEditorInput input= (ClassFileEditorInput) getEditorInput();
				IJavaElement element= input.getClassFile().getElementAt(position);
				if (element instanceof ISourceReference)
					return (ISourceReference) element;
			} catch (JavaModelException x) {
			}
		}
		return null;
	}
	
	/**
	 * @see EditorPart#init(IEditorSite, IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof ClassFileEditorInput))
			throw new PartInitException(getResourceString("Editor.Error.invalid_input"));
			
		super.init(site, input);
	}
	
	/**
	 * @see IEditorPart#saveState(IMemento)
	 */
	public void saveState(IMemento memento) {
	}
		
	/**
	 * @see AbstractTextEditor#editorContextMenuAboutToShow
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		
		super.editorContextMenuAboutToShow(menu);
		
		if (getEditorInput() instanceof ClassFileEditorInput) {
			
			ClassFileEditorInput input= (ClassFileEditorInput) getEditorInput();
			IClassFile file= input.getClassFile();
			
			try {
				if (file.getSource() != null)
					addAction(menu, ITextEditorActionConstants.GROUP_ADD, "AddBreakpoint");
			} catch (JavaModelException x) {
				// ignore
			}
		}
	}
	
	/**
	 * @see AbstractTextEditor#rulerContextMenuAboutToShow
	 */
	protected void rulerContextMenuAboutToShow(IMenuManager menu) {
		super.rulerContextMenuAboutToShow(menu);
		
		if (getEditorInput() instanceof ClassFileEditorInput) {
			
			ClassFileEditorInput input= (ClassFileEditorInput) getEditorInput();
			IClassFile file= input.getClassFile();
			
			try {
				if (file.getSource() != null)
					addAction(menu, "ManageBreakpoints");
			} catch (JavaModelException x) {
				// ignore
			}
		}
	}
	
	/**
	 * @see AbstractTextEditor#isEditable()
	 */
	public boolean isEditable() {
		return false;
	}

	/**
	 * @see JavaEditor#setOutlinePageInput(JavaOutlinePage, IEditorInput)
	 */
	protected void setOutlinePageInput(JavaOutlinePage page, IEditorInput input) {
		if (page != null && input instanceof ClassFileEditorInput) {
			ClassFileEditorInput cfi= (ClassFileEditorInput) input;
			page.setInput(cfi.getClassFile());
		}
	}
}