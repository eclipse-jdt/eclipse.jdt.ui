/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;import org.eclipse.jface.util.Assert;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.wizard.WizardDialog;import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;import org.eclipse.ui.texteditor.IUpdate;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;import org.eclipse.jdt.internal.ui.refactoring.code.ExtractMethodWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * Extracts a new method from the text editor's text selection by using the
 * extract method refactoing.
 */
public class ExtractMethodAction extends Action implements IUpdate, IWorkbenchWindowActionDelegate {

	private JavaEditor fEditor;
	private IAction fAction;
	private IWorkbenchWindow fWorkbenchWindow;
	
	/**
	 * Creates a new extract method action when used as an action delegate.
	 */
	public ExtractMethodAction() {
		super(RefactoringMessages.getString("ExtractMethodAction.extract_method")); //$NON-NLS-1$
	}
	
	/**
	 * Creates a new extract method action for the given text editor. The text
	 * editor's selection marks the set of statements to be extracted into a new
	 * method.
	 * @param editor the text editor.
	 */
	public ExtractMethodAction(JavaEditor editor) {
		this();
		fEditor= editor;
		Assert.isNotNull(fEditor);
	}

	/* (non-JavaDoc)
	 * Method declared in IUpdate.
	 */
	public void update() {
		setEnabled(canOperateOn());
	}
	
	/* (non-JavaDoc)
	 * Method declared in IAction.
	 */
	public void run() {
		ICompilationUnit cu= getCompilationUnit();
		ITextSelection selection= getTextSelection();
		ExtractMethodWizard wizard= new ExtractMethodWizard(cu, selection, fEditor.getDocumentProvider());
		WizardDialog dialog= new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard);
		dialog.open();			
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
			fEditor= (JavaEditor)part;
			if (!canOperateOn()) {
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), 
					"Extract Method Refactoring",
					"Cannot perform extract method on current text selection.");
				return;
			}
		} else {
			MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), 
				"Extract Method Refactoring",
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
				fEditor= null;
				if (part instanceof JavaEditor) {
					fEditor= (JavaEditor)part;
					enabled= true;
				}
				if (fAction != null)
					fAction.setEnabled(enabled);
			}
			public void partBroughtToTop(IWorkbenchPart part) {
			}
			public void partClosed(IWorkbenchPart part) {
				if (part == fEditor && fAction != null)
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
		if (fEditor == null)
			return false;
		ISelection selection= fEditor.getSelectionProvider().getSelection();
		if (!(selection instanceof ITextSelection))
			return false;
			
		return ((ITextSelection)selection).getLength() > 0;
	}
	
	private ICompilationUnit getCompilationUnit() {
		return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
	}
	
	private ITextSelection getTextSelection() {
		return (ITextSelection)fEditor.getSelectionProvider().getSelection();
	}	
}