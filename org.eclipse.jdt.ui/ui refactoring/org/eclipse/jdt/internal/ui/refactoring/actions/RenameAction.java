package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class RenameAction extends SelectionDispatchAction {

	private RenameJavaElementAction fRenameJavaElement;
	private RenameTempAction fRenameTemp;

	private CompilationUnitEditor fEditor;
	
	public RenameAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("RenameAction.text")); //$NON-NLS-1$
		fRenameJavaElement= new RenameJavaElementAction(site);
		fRenameJavaElement.setText(getText());
	}
	
	public RenameAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		fRenameTemp= new RenameTempAction(fEditor);
		fRenameJavaElement= new RenameJavaElementAction(editor);
	}
	
	/*
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		fRenameJavaElement.selectionChanged(event);
		if (fRenameTemp != null)
			fRenameTemp.selectionChanged(event);
		setEnabled(computeEnabledState());		
	}

	/*
	 * @see IUpdate#update()
	 */
	public void update() {
		fRenameJavaElement.update();
		
		if (fRenameTemp != null)
			fRenameTemp.update();
	
		setEnabled(computeEnabledState());		
	}
	
	private boolean computeEnabledState(){
		if (fRenameTemp != null)	
			return fRenameTemp.isEnabled() || fRenameJavaElement.isEnabled();
		else
			return fRenameJavaElement.isEnabled();
	}
	
	protected void run(IStructuredSelection selection) {
		 if (fRenameJavaElement.isEnabled())
			fRenameJavaElement.run(selection);
	}

	protected void run(ITextSelection selection) {
		if (fRenameTemp != null && fRenameTemp.canRun(selection))
			fRenameTemp.run(selection);
		else if (fRenameJavaElement.canRun(selection))
			fRenameJavaElement.run(selection);
		else
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("RenameAction.rename"), RefactoringMessages.getString("RenameAction.unavailable"));  //$NON-NLS-1$ //$NON-NLS-2$
	}
}
