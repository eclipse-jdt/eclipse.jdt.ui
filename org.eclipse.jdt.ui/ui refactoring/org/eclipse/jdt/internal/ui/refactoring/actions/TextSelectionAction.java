package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

abstract public class TextSelectionAction extends Action implements IUpdate, IWorkbenchWindowActionDelegate {
	
	private JavaEditor fEditor;
	private IAction fAction;
	private IWorkbenchWindow fWorkbenchWindow;

	private String fOperationNotAvailableDialogMessage;
	private String fOperationNotAvailableDialogTitle;
	
	/**
	 * Creates a new action when used as an action delegate.
	 */
	public TextSelectionAction(String name) {
		this(name, "Refactoring", "Cannot perform this action on the current text selection.");
	}
	
	/**
	 * Creates a new action when used as an action delegate.
	 */
	protected TextSelectionAction(String name, String operationNonAvailableDialogTitle, String operationNonAvailableDialogMessage) {
		super(name);
		Assert.isNotNull(operationNonAvailableDialogTitle);
		Assert.isNotNull(operationNonAvailableDialogMessage);
		fOperationNotAvailableDialogTitle= operationNonAvailableDialogTitle;
		fOperationNotAvailableDialogMessage= operationNonAvailableDialogMessage;
	}
	
	/* (non-JavaDoc)
	 * Method declared in IUpdate.
	 */
	public void update() {
		setEnabled(canOperateOnCurrentSelection());
	}
	
	//-- accessors ---
	
	protected final JavaEditor getEditor() {
		return fEditor;
	}

	protected final void setEditor(JavaEditor editor) {
		//Assert.isNotNull(editor);
		this.fEditor= editor;
	}
	
	protected final ICompilationUnit getCompilationUnit() {
		return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(getEditor().getEditorInput());
	}
	
	protected final ITextSelection getTextSelection() {
		return (ITextSelection)getEditor().getSelectionProvider().getSelection();
	}
	
	protected final String getOperationNotAvailableDialogTitle() {
		return fOperationNotAvailableDialogTitle;
	}	
	
	//---- IWorkbenchWindowActionDelegate stuff ----------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void run(IAction action) {
		if (fAction == null)
			fAction= action;
		IWorkbenchPart part= fWorkbenchWindow.getPartService().getActivePart();
		if (part instanceof JavaEditor) {
			setEditor((JavaEditor)part);
			if (!canOperateOnCurrentSelection()) {
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), 
					fOperationNotAvailableDialogTitle,
					fOperationNotAvailableDialogMessage);
				return;
			}
		} else {
			MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), 
				getText(),
				"Active part is not a Java Editor.");
			fAction.setEnabled(false);
			return;
		}
		run();
	}
	
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection s) {
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void dispose() {
		fAction= null;
		fWorkbenchWindow= null;
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		fWorkbenchWindow= window;
		window.getPartService().addPartListener(new IPartListener() {
			public void partActivated(IWorkbenchPart part) {
				boolean enabled= false;
				setEditor(null);
				if (part instanceof JavaEditor) {
					setEditor((JavaEditor)part);
					enabled= true;
				}
				if (fAction != null)
					fAction.setEnabled(enabled);
			}
			public void partBroughtToTop(IWorkbenchPart part) {
			}
			public void partClosed(IWorkbenchPart part) {
				if (part == getEditor() && fAction != null)
					fAction.setEnabled(false);
			}
			public void partDeactivated(IWorkbenchPart part) {
			}
			public void partOpened(IWorkbenchPart part) {
			}
		});
	}	
		
	protected boolean canOperateOnCurrentSelection() {
		if (getEditor() == null)
			return false;
		ISelection selection= getEditor().getSelectionProvider().getSelection();
		if (!(selection instanceof ITextSelection))
			return false;
		
		if (getCompilationUnit() == null)
			return false;		
		
		return (((ITextSelection)selection).getLength() > 0);
	}	
}

