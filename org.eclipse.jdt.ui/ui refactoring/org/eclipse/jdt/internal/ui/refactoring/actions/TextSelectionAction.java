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
	
	/**
	 * Creates a new action when used as an action delegate.
	 */
	public TextSelectionAction(String name) {
		super(name);
	}
	
	/* (non-JavaDoc)
	 * Method declared in IUpdate.
	 */
	public void update() {
		setEnabled(canOperateOn());
	}
	
	//-- hooks --
	protected String getDialogTitle(){
		return getText();
	}
	
	protected boolean canOperateOnEmptySelection(){
		return false;
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
			if (!canOperateOn()) {
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), 
					getDialogTitle(),
					"Cannot perform this action on the current text selection.");
				return;
			}
		} else {
			MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), 
				getDialogTitle(),
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
	
	//---- private helpers --------------------------------------------------------------------------
	
	private boolean canOperateOn() {
		if (getEditor() == null)
			return false;
		ISelection selection= getEditor().getSelectionProvider().getSelection();
		if (!(selection instanceof ITextSelection))
			return false;
		
		if (getCompilationUnit() == null)
			return false;		
		
		Assert.isTrue(((ITextSelection)selection).getLength() >= 0);	
		
		if (((ITextSelection)selection).getLength() == 0)
			return canOperateOnEmptySelection();
		
		return true;	
	}	
}

