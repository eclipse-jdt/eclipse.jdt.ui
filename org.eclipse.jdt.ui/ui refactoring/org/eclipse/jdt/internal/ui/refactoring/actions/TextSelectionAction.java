package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

abstract public class TextSelectionAction extends Action implements IUpdate, IWorkbenchWindowActionDelegate {
	
	private AbstractTextEditor fEditor;
	private Class fEditorClass;
	private IWorkbenchWindow fWorkbenchWindow;

	/**
	 * Creates a new action when used as an action delegate.
	 */
	protected TextSelectionAction(String name) {
		super(name);
		fEditorClass= CompilationUnitEditor.class;
	}
	
	/* (non-JavaDoc)
	 * Method declared in IUpdate.
	 */
	public void update() {
		setEnabled(canOperateOnCurrentSelection());
	}
	
	//---- Accessors ------------------------------------------------------------------------------------------

	public final void setEditor(AbstractTextEditor editor) {
		Assert.isNotNull(editor);
		if (fEditorClass.isInstance(editor))
			fEditor= editor;
		else
			fEditor= null;
	}	
	
	protected final AbstractTextEditor getEditor() {
		if (fEditor != null)
			return fEditor;
		IWorkbenchPart part= fWorkbenchWindow.getPartService().getActivePart();
		if (part instanceof AbstractTextEditor && fEditorClass.isInstance(part))
			return (AbstractTextEditor)part;
		return null;
	}

	protected final void setEditorClass(Class clazz) {
		Assert.isNotNull(clazz);
		fEditorClass= clazz;
	}
	
	protected final ICompilationUnit getCompilationUnit() {
		return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(getEditor().getEditorInput());
	}
	
	protected final ITextSelection getTextSelection() {
		return (ITextSelection)getEditor().getSelectionProvider().getSelection();
	}
	
	//---- IWorkbenchWindowActionDelegate stuff ----------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void run(IAction action) {
		run();
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection s) {
		boolean enabled= false;
		if (getEditor() != null)
			enabled= canOperateOnCurrentSelection(s);
		action.setEnabled(enabled);
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void dispose() {
		fWorkbenchWindow= null;
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		fWorkbenchWindow= window;
	}	
		
	protected boolean canOperateOnCurrentSelection() {
		if (getEditor() == null)
			return false;
			
		return canOperateOnCurrentSelection(getEditor().getSelectionProvider().getSelection());
	}
	
	protected boolean canOperateOnCurrentSelection(ISelection selection) {
		if (!(selection instanceof ITextSelection))
			return false;
		
		return (((ITextSelection)selection).getLength() > 0);
	}
}

