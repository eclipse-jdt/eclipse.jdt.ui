package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class RenameAction extends SelectionDispatchAction {

	private RenameJavaElementAction fRename1;
	private RenameTempAction fRenameTemp;

	private CompilationUnitEditor fEditor;
	
	public RenameAction(UnifiedSite site) {
		super(site);
		setText(RefactoringMessages.getString("RenameAction.text")); //$NON-NLS-1$
		fRename1= new RenameJavaElementAction(site);
		fRename1.setText(getText());
	}
	
	public RenameAction(CompilationUnitEditor editor) {
		this(UnifiedSite.create(editor.getEditorSite()));
		fEditor= editor;
		fRenameTemp= new RenameTempAction(fEditor);
		
		fRename1= new RenameJavaElementAction(editor);
		
	}
	
	/*
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		fRename1.selectionChanged(event);
		if (fRenameTemp != null)
			fRenameTemp.selectionChanged(event);
		setEnabled(computeEnabledState());		
	}

	/*
	 * @see IUpdate#update()
	 */
	public void update() {
		fRename1.update();
		
		if (fRenameTemp != null)
			fRenameTemp.update();
	
		setEnabled(computeEnabledState());		
	}
	
	private boolean computeEnabledState(){
		if (fRenameTemp != null)	
			return fRenameTemp.isEnabled() || fRename1.isEnabled();
		else
			return fRename1.isEnabled();
	}
	
	protected void run(IStructuredSelection selection) {
		 if (fRename1.isEnabled())
			fRename1.run(selection);
	}

	protected void run(ITextSelection selection) {
		if (fRenameTemp != null && fRenameTemp.canRun(selection))
			fRenameTemp.run(selection);
		else if (fRename1.canRun(selection))
			fRename1.run(selection);
		else
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("RenameAction.rename"), RefactoringMessages.getString("RenameAction.unavailable"));  //$NON-NLS-1$ //$NON-NLS-2$
	}
}
